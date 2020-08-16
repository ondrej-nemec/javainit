package socketCommunication.http.server.session;

import java.io.File;
import java.io.IOException;

import core.text.Text;
import core.text.basic.ReadText;
import core.text.basic.WriteText;

public class FileSessionStorage implements SessionStorage {
	
	private final String sessionPath;
	
	public FileSessionStorage(String sessionPath) {
		this.sessionPath = sessionPath;
	}

	@Override
	public Session getSession(String sessionId) {
		try {
			return Session.deserialize(Text.read((br)->{
				return ReadText.asString(br);
			}, getFileName(sessionId)));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void addSession(Session session) {
		try {
			Text.write((bw)->{
				WriteText.write(bw, session.serialize());
			}, getFileName(session.getSessionId()), false);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void removeSession(String sessionId) {
		new File(getFileName(sessionId)).delete();
	}

	private String getFileName(String name) {
		return String.format("%s/%s.session", sessionPath, name);
	}
	
}
