package socketCommunication.http.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import common.Logger;
import common.structures.DictionaryValue;
import json.JsonReader;
import json.JsonStreamException;
import socketCommunication.Servant;
import socketCommunication.http.HttpMethod;
import socketCommunication.http.server.profiler.HttpServerProfiler;
import socketCommunication.http.server.profiler.HttpServerProfilerEvent;

public class RestApiServer implements Servant {
	
	public static HttpServerProfiler PROFILER = null;
	
	protected final static String METHOD = "method";
	protected final static String PROTOCOL = "protocol";
	protected final static String URL = "url";
	protected final static String FULL_URL = "full-url";
		
	private final Logger logger;
	private final int maxUploadFileSize;
	private final Optional<List<String>> allowedFileTypes;
	private final RestApiServerResponseFactory createResponce;
	
	public RestApiServer(
			RestApiServerResponseFactory response,
			int maxUploadFileSize,
			Optional<List<String>> allowedFileTypes,
			Logger logger) {
		this.logger = logger;
		this.createResponce = response;
		this.allowedFileTypes = allowedFileTypes;
		this.maxUploadFileSize = maxUploadFileSize;
	}
	
	@Override
	public void serve(Socket socket, String charset) throws IOException {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream(), charset));
           	 BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), charset));
           	 BufferedInputStream is = new BufferedInputStream(socket.getInputStream());
           	 BufferedOutputStream os = new BufferedOutputStream(socket.getOutputStream());) {
			serve(br, bw, is, os, socket.getInetAddress().toString());
       }
	}
	
	protected void serve(
			BufferedReader br, 
			BufferedWriter bw,
			BufferedInputStream is,
			BufferedOutputStream os,
			String clientIp) throws IOException {
		profile(HttpServerProfilerEvent.REQUEST_ACCEPT);
		RequestParameters params = new RequestParameters();
		Properties header = new Properties();
		Properties request = new Properties();
		parseRequest(request, header, params, br, is);
		profile(HttpServerProfilerEvent.REQUEST_PARSED);
		/***********/
		logger.debug("Request: " + request);
		RestApiResponse response = createResponce.accept(
			HttpMethod.valueOf(request.getProperty(METHOD).toUpperCase()),
			request.getProperty(URL),
			request.getProperty(FULL_URL),
			request.getProperty(PROTOCOL),
			header,
			params,
			clientIp
		);
		profile(HttpServerProfilerEvent.RESPONSE_CREATED);
		sendResponse(response, request, bw, os);
		profile(HttpServerProfilerEvent.RESPONSE_SENDED);
	}
	
	private void profile(HttpServerProfilerEvent event) {
		if (PROFILER != null) {
			PROFILER.log(event);
		}
	}
	
	private void sendResponse(
			RestApiResponse response,
			Properties request, 
			BufferedWriter bw,
			BufferedOutputStream os
		) throws IOException {
		String code = response.getStatusCode().toString();
		logger.debug("Response: " + code);
				
		// write first line
		bw.write(request.getProperty(PROTOCOL));
        bw.write(" ");
        bw.write(code);
        bw.newLine();
		// write headers
        for (String headerLine : response.getHeader()) {
        	bw.write(headerLine);
        	bw.newLine();
        }
		// end of header
        bw.newLine();
        bw.flush();
		// write binary context
        response.createBinaryContent(os);
		// write text context
        response.createTextContent(bw);
	}

	/********* PARSE **************/
	
	private void parseRequest(
			Properties request, Properties header, RequestParameters params, 
			BufferedReader br, BufferedInputStream bis) throws IOException {
		// url, method, protocol
		String first = br.readLine();
		if (first == null) {
			logger.warn("Wrong request - empty first line");
			return;
		}
		parseFirst(request, params, first);
        // header
        String line = br.readLine();
        while (line != null && !line.isEmpty()) {
        	parseHeaderLine(line, header);
        	line = br.readLine();
        }
        String type = header.getProperty("Content-Type");
        if (type != null && type.contains("multipart/form-data")) {
			parseFileForm(type, header, params, br, bis);
		} else {
	        // payload
	        StringBuilder payload = new StringBuilder();
	       	// stream ready is fix - before close stream does not wrote -1
	        // using header Content-Length is not reliable - does not works f.e. with arabic chars
	        if (br.ready()) {
	            int value;
	            while((value = br.read()) != -1) {
	                payload.append((char) value);
	                if (!br.ready()) {
	                    break;
	                }
	            }
	        }
	        if (type != null && type.contains("application/json")) {
	        	try {
					params.putAll(new DictionaryValue(new JsonReader().read(payload.toString())).getMap());
				} catch (JsonStreamException e) {
					throw new IOException(e);
				}
	        } else if (type != null && type.contains("text/plain")) {
	        	params.setPlainBody(payload.toString());
	        } else if (type != null && type.contains("application/javascript")) { // TODO parser
	        	params.setPlainBody(payload.toString());
	        } else if (type != null && type.contains("text/html")) { // TODO parser
	        	params.setPlainBody(payload.toString());
	        } else if (type != null && type.contains("application/xml")) { // TODO parser
	        	params.setPlainBody(payload.toString());
	        } else {
	        	parsePayload(params, payload.toString());
	        }
		}
	}
	
	protected void parseFileForm(
			String type,
			Properties header, RequestParameters params,
			BufferedReader br, BufferedInputStream bis) throws IOException {
		int contentLength = Integer.parseInt(header.getProperty("Content-Length"));
		
		boolean containsFile = bis.available() > 0;
		String boundary = "--" + type.split(";")[1].split("=")[1].trim();
		
		int readed = 0;
		boolean isElementValue = false;
		String elementName = null;
		String elementValue = null;
		String contentType = null;
		String filename = null;
		ByteArrayOutputStream fileContent = null;
		while(readed < contentLength) {
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			byte[] bytes;
			String requestLine;
			if (containsFile) {
				while (readed < contentLength) {
					if (bis.available() == 0) {
						throw new IOException("Content length not match expecation. Expected: " + contentLength + ", actual: " + readed);
	                }
					byte actual = (byte)bis.read();
					readed++;
					stream.write(actual);
					if (actual == '\n') {
						break;
					}
				}
				bytes = stream.toByteArray();
				requestLine = new String(bytes);
			} else {
				StringBuilder builder = new StringBuilder();
				while (readed < contentLength) {
					if (!br.ready()) {
						throw new IOException("Content length not match expecation. Expected: " + contentLength + ", actual: " + readed);
	                }
					char actual = (char)br.read();
					readed += (actual+"").getBytes().length;
					builder.append(actual);
					if (actual == '\n') {
						break;
					}
				}
				requestLine = builder.toString();
				bytes = null;
			}
			requestLine = requestLine.replace("\r", "").replace("\n", "");
			
			if (requestLine.startsWith(boundary)) {
				if (elementName != null && elementValue != null) {
					parseParams(params, elementName, elementValue);
				} else if (elementName != null && fileContent != null) {
					params.put(elementName, new UploadedFile(filename, contentType, fileContent));
				}
			}
			if (boundary.equals(requestLine)) {
				isElementValue = false;
				elementName = null;
				contentType = null;
				filename = null;
				elementValue = null;
				fileContent = null;
			} else if (requestLine.isEmpty() && fileContent != null && isElementValue && bytes.length == 1) {
				// if bytes.lenght == 2 => start or end
				// if isElementValue == false => start
				fileContent.write(bytes); // fix
			} else if (requestLine.isEmpty()) {
				isElementValue = true;
			} else if (isElementValue) {
				if (filename == null) { // text element
					if (elementValue == null) {
						elementValue = "";
					} else {
						elementValue += "\n";
					}
					elementValue += requestLine;
				} else { // file
					if (fileContent == null) {
						fileContent = new ByteArrayOutputStream();
					}
					if (bytes.length + fileContent.size() > maxUploadFileSize) {
						throw new IOException("Maximal upload file size overflow " + maxUploadFileSize);
					}
					fileContent.write(bytes);
					fileContent.flush();
				}
			} else if (requestLine.startsWith("Content-Disposition: form-data; name=") && !isElementValue) {
				Matcher m = Pattern.compile(" name=\\\"(([^\\\"])+)\\\"(; (filename=\\\"(([^\\\"])+)\\\")?)?")
						.matcher(requestLine);
				m.find();
				elementName = m.group(1);
				filename = m.group(5);
				if (filename != null && (filename.contains("..") || filename.contains("/") || filename.contains("\\"))) {
					throw new IOException("Filename is probably corrupted " + filename);
				}
			} else if (requestLine.startsWith("Content-Type: ") && !isElementValue) {
				contentType = requestLine.replace("Content-Type: ", "");
				if (allowedFileTypes.isPresent() && !allowedFileTypes.get().contains(contentType)) {
					throw new IOException("Uploading file content type is not allowed " + contentType);
				}
			}
        }
	}

	/** protected for test only */
	protected void parseFirst(Properties request, RequestParameters params, String first) throws UnsupportedEncodingException {
		String[] methods = first.split(" ");
		if (methods.length != 3) {
			logger.warn("Invalid request: " + first);
			return; //TODO what now?
		}
		
		String[] urlParst = methods[1].split("\\?");
		if (urlParst.length > 1) {
			parsePayload(params, urlParst[1]);
		}		
		
		request.put(METHOD, methods[0]);
		request.put(URL, urlParst[0]);
		request.put(FULL_URL, methods[1]);
		request.put(PROTOCOL, methods[2]);
	}

	/** protected for test only */
	protected void parseHeaderLine(String line, Properties header) {
		if (line.isEmpty()) {
			return;
		}
		String[] property = line.split(": ", 2);
    	if (property.length == 2 && ! property[0].isEmpty()) {
    		header.put(property[0], property[1]);
    	} else if (property.length == 1){
    		header.put(property[0], "");
    	} else {
    		logger.warn("Invalid header line " + line);
    	}
	}

	/** protected for test only */
	protected void parsePayload(RequestParameters prop, String payload) throws UnsupportedEncodingException {
		if (payload.isEmpty()) {
			return;
		}
		/*
		String url = URLDecoder.decode(payload, StandardCharsets.UTF_8.toString());
		String[] params = url.split("\\&");
		/*/
		String[] params = payload.split("\\&");
		//*/
		for (String param : params) {
			String[] keyValue = param.split("=");
			if (keyValue.length == 1) {
				parseParams(prop, keyValue[0], "");
				//prop.put(keyValue[0], "");
			} else if (keyValue.length == 2) {
				parseParams(prop, keyValue[0], keyValue[1]);
				//prop.put(keyValue[0], keyValue[1]);
			} else {
		    	logger.warn("Invalid param " + param);
		    }
		}
	}

	private void parseParams(RequestParameters params, String key, String value) throws UnsupportedEncodingException {
		//key = key.replace("[]", "&=").replace("][", "&").replace("[", "&").replace("]", "&");
		key = URLDecoder.decode(key, StandardCharsets.UTF_8.toString());
		key = key.replace("[]", "[=]").replace("[", "&").replace("]", "");
		String[] keys = key.split("\\&");
		value = URLDecoder.decode(value, StandardCharsets.UTF_8.toString());
		int keyCount = StringUtils.countMatches(key, "&");
		if (keyCount == 0) {
			params.put(key, value);
			return;
		}
		parseParams(params, keys, value);
	}

	private void parseParams(RequestParameters params, String[] keys, String value) throws UnsupportedEncodingException {
		String key = keys[0]; // URLDecoder.decode(keys[0], StandardCharsets.UTF_8.toString());
		Object o = params.getValue(key);
		if (o == null && keys[1].equals("=")) {
			params.put(key, new LinkedList<>());
		} else if (o == null) {
			params.put(key, new HashMap<>());
		}
		parseParams(params.getValue(key), keys, 1, value);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void parseParams(Object o, String[] keys, int index, String value) throws UnsupportedEncodingException {
		if (index == keys.length) {
			return;
		}
		
		Object sub = null;
		if (o instanceof Map) {
			// sub = Map.class.cast(o).get(URLDecoder.decode(keys[index], StandardCharsets.UTF_8.toString()));
			sub = Map.class.cast(o).get(keys[index]);
		} else if (o instanceof List) {
			List l = List.class.cast(o);
			if (!l.isEmpty()) {
				sub = l.get(l.size() - 1);
			}
		}
		boolean needInsert = false;
		if (index == keys.length - 1) {
			needInsert = true;
			sub = value;
		} else if (sub == null && keys[index+1].equals("=")) {
			needInsert = true;
			sub = new LinkedList<>();
		} else if (sub == null) {
			needInsert = true;
			sub = new HashMap<>();
		}
		if (needInsert && o instanceof Map) {
			Map.class.cast(o).put(keys[index], sub);
		} else if (needInsert && o instanceof List) {
			List.class.cast(o).add(sub);
		}
		parseParams(sub, keys, index+1, value);
	}
	
}
