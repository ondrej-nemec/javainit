package socketCommunication.http.server;

import static common.structures.MapInit.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;


import org.junit.Test;
import org.junit.runner.RunWith;

import common.Logger;
import common.structures.Tuple2;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import socketCommunication.http.server.RestApiServerResponseFactory;
import socketCommunication.http.server.RestApiServer;

@RunWith(JUnitParamsRunner.class)
public class RestApiServerTest {

	// TODO test file upload
	
	@Test
	@Parameters(method = "dataParseFirstSplitFirstLine")
	public void testParseFirstSplitFirstLine(String line, Properties expectedRequest, RequestParameters expectedParams) throws UnsupportedEncodingException {
		RestApiServer api = getApi();
		
		Properties actualRequest = new Properties();
		RequestParameters actualParams = new RequestParameters();
		api.parseFirst(actualRequest, actualParams, line);
		
		assertEquals(expectedRequest, actualRequest);
		assertEquals(expectedParams, actualParams);
	}
	
	public Object[] dataParseFirstSplitFirstLine() {
		return new Object[] {
				new Object[] {
						"",
						properties(),
						new RequestParameters()
				},
				new Object[] {
						"method url protocol",
						properties(
								new Tuple2<>(RestApiServer.METHOD, "method"),
								new Tuple2<>(RestApiServer.URL, "url"),
								new Tuple2<>(RestApiServer.FULL_URL, "url"),
								new Tuple2<>(RestApiServer.PROTOCOL, "protocol")
						),
						new RequestParameters()
				},
				new Object[] {
						"method url?aa=bb protocol",
						properties(
								new Tuple2<>(RestApiServer.METHOD, "method"),
								new Tuple2<>(RestApiServer.URL, "url"),
								new Tuple2<>(RestApiServer.FULL_URL, "url?aa=bb"),
								new Tuple2<>(RestApiServer.PROTOCOL, "protocol")
						),
						new RequestParameters(new Tuple2<>("aa", "bb"))
				},
		};
	}
	
	@Test
	@Parameters(method = "dataParseHeaderLineFillHeaderProperties")
	public void testParseHeaderLineFillHeaderProperties(Properties expected, String line) {
		RestApiServer api = getApi();
		Properties actual = new Properties();
		api.parseHeaderLine(line, actual);
		assertEquals(expected, actual);
	}
	
	public Object[] dataParseHeaderLineFillHeaderProperties() {
		return new Object[] {
			new Object[] {
				properties(new Tuple2<>("aa", "bb")),
				"aa: bb"
			},
			new Object[] {
				properties(),
				""
			},
			new Object[] {
				properties(),
				": "
			},
			new Object[] {
				properties(new Tuple2<>("aa", "bb: ")),
				"aa: bb: "
			},
			new Object[] {
				properties(new Tuple2<>("aa", "")),
				"aa: "
			},
			new Object[] {
				properties(new Tuple2<>("aa", "")),
				"aa"
			},
		};
	}
	
	@Test
	@Parameters(method = "dataParsePayloadFillParamsProperties")
	public void testParsePayloadFillParamsProperties(RequestParameters expected, String payload) throws UnsupportedEncodingException {
		RestApiServer api = getApi();
		RequestParameters actual = new RequestParameters();
		api.parsePayload(actual, payload);
		assertEquals(expected, actual);
	}
	
	public Object[] dataParsePayloadFillParamsProperties() {
		return new Object[] {
			new Object[] {
				new RequestParameters(new Tuple2<>("aa", "bb")),
				"aa=bb"
			},
			new Object[] {
				new RequestParameters(),
				""
			},
			new Object[] {
				new RequestParameters(),
				"&"
			},
			new Object[] {
				new RequestParameters(),
				"=&="
			},
			new Object[] {
				new RequestParameters(new Tuple2<>("aa", "bb")),
				"aa=bb&"
			},
			new Object[] {
				new RequestParameters(new Tuple2<>("aa", "")),
				"aa="
			},
			new Object[] {
					new RequestParameters(new Tuple2<>("aa", "")),
					"aa"
				},
			new Object[] {
				new RequestParameters(new Tuple2<>("aa", "bb"), new Tuple2<>("cc", "dd")),
				"aa=bb&cc=dd"
			},
			new Object[] {
				new RequestParameters(new Tuple2<>("dd", "ee")),
				"aa=bb=cc&dd=ee"
			},
			/*************
			new Object[] {
					new RequestParameters(new Tuple2<>("number", 1)),
					"number=1"
				},
			new Object[] {
					new RequestParameters(new Tuple2<>("boolean", true)),
					"boolean=true"
				},
			new Object[] {
					new RequestParameters(new Tuple2<>("string", "text")),
					"string=text"
				},
			new Object[] {
					new RequestParameters(new Tuple2<>("double", 12.3)),
					"double=12.3"
				},*/
			new Object[] {
					new RequestParameters(new Tuple2<>("aa", "bb"), new Tuple2<>("%", "&"), new Tuple2<>("dd", "ee")),
					"aa=bb&%25=%26&dd=ee"
				},
			new Object[] {
					new RequestParameters(new Tuple2<>("list", Arrays.asList("true", "b", "3"))),
					"list%5B%5D=true&list%5B%5D=b&list%5B%5D=3"
				},
			new Object[] {
					new RequestParameters(new Tuple2<>("list", Arrays.asList("true", "b", "3", ""))),
					"list%5B%5D=true&list%5B%5D=b&list%5B%5D=3&list%5B%5D="
				},
			new Object[] {
					new RequestParameters(new Tuple2<>("list", Arrays.asList(Arrays.asList("true", "b", "3")))),
					"list%5B%5D%5B%5D=true&list%5B%5D%5B%5D=b&list%5B%5D%5B%5D=3"
				},
			new Object[] {
					new RequestParameters(new Tuple2<>("array", hashMap(
						new Tuple2<>("first", "true"),
						new Tuple2<>("second", "b"),
						new Tuple2<>("third", "3")
					))),
					"array%5Bfirst%5D=true&array%5Bsecond%5D=b&array%5Bthird%5D=3"
				},
			new Object[] {
					new RequestParameters(new Tuple2<>("array", hashMap(
						new Tuple2<>("abc", hashMap(
								new Tuple2<>("first", true),
								new Tuple2<>("second", "b"),
								new Tuple2<>("third", 3),
								new Tuple2<>("four", "")
						))
					))),
					"array%5Babc%5D%5Bfirst%5D=true&array%5Babc%5D%5Bsecond%5D=b&array%5Babc%5D%5Bthird%5D=3&array%5Babc%5D%5Bfour%5D="
				}
		};
	}
	/*
	@Test
	public void test() throws UnsupportedEncodingException {
		List<String> first = new LinkedList<>();
		first.addAll(Arrays.asList("1", "2", "3"));
		List<String> second = new LinkedList<>();
		second.addAll(Arrays.asList("7", "8", "9"));
		RequestParameters expected = new RequestParameters(
				new Tuple2<>("aa", first),
				new Tuple2<>("b", MapInit.hashMap(
					new Tuple2<>("a", "4"),
					new Tuple2<>("b", "5"),
					new Tuple2<>("c", "6")
				)),
				new Tuple2<>("c", MapInit.hashMap(
					new Tuple2<>("a", second)
				))
		);
		String payload = "aa[]=1&aa[]=2&aa[]=3&b[a]=4&b[b]=5&b[c]=6&c[a][]=7&c[a][]=8&c[a][]=9";
		RestApiServer api = getApi();
		RequestParameters actual = new RequestParameters();
		api.parsePayload(actual, payload);
		System.out.println(expected);
		System.out.println(actual);
		//System.out.println();
		assertEquals(expected, actual);
	}
	//*/
	/***********************/
	
	private RestApiServer getApi() {
		return new RestApiServer(
				mock(RestApiServerResponseFactory.class),
				0,
				Optional.empty(),
				mock(Logger.class)
		);
	}

}
