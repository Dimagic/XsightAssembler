package xsightassembler.utils;

import com.jcraft.jsch.*;
import javafx.concurrent.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xsightassembler.MainApp;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class JSSHClient implements MsgBox {
	private static final Logger LOGGER = LogManager.getLogger(JSSHClient.class.getName());
	private Map<String,String> cmdPaths = new HashMap<>();
	private MainApp mainApp;
	private String host;
	private String user;
	private String password;
	private JSch jsch;
	private Session session;
	private Channel channel;
	private BiTestWorker testWorker;

	public JSSHClient(String host, String user, String password) {
		this.host = host;
		this.user = user;
		this.password = password;
	}

	public JSSHClient(String host, String user, String password, BiTestWorker testWorker) {
		this.host = host;
		this.user = user;
		this.password = password;
		this.testWorker = testWorker;
	}

	public HashMap<String, String> send(String command) throws CustomException {
		HashMap<String, String> result = new HashMap<String, String>();
		try {
//			command = findPathCmd(command);
			session = getSession();
			if (!session.isConnected()) {
				return null;
			}

			Channel channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);
			channel.setInputStream(null);
			((ChannelExec) channel).setErrStream(System.err);

			InputStream in = channel.getInputStream();
			channel.connect();

			StringBuilder builder = new StringBuilder();
			byte[] tmp = new byte[1024];
			while (true) {
				while (in.available() > 0) {
					int i = in.read(tmp, 0, 1024);
					if (i < 0)
						break;
					builder.append(new String(tmp, 0, i));
//					if (testWorker != null){
//						testWorker.updateCurrentMessage(new String(tmp, 0, i));
//					}
				}
				if (channel.isClosed()) {
					result.put("result", builder.toString());
					result.put("exit-status", Integer.toString(channel.getExitStatus()));
					break;
				}

			}
			channel.disconnect();
			session.disconnect();
		} catch (Exception e) {
			LOGGER.error("exeption", e);
			throw new CustomException(e);
		}
		return result;
	}

	private Session getSession() throws JSchException {
		if (session != null && session.isConnected()) {
			return session;
		} else {
			Properties config = new Properties();
			config.put("StrictHostKeyChecking", "no");
			jsch = new JSch();
			session = jsch.getSession(user, host, 22);
			session.setPassword(password);
			session.setConfig(config);
			session.connect();
			return session;
		}
	}

	private String findPathCmd(String cmd) throws Exception {
		String cmdForSearch = cmd.split(" ")[0];
		if (cmdPaths.get(cmdForSearch) != null){
			return cmd.replace(cmdForSearch, cmdPaths.get(cmdForSearch));
		}
		if (cmdForSearch.equals("find")) {
			return cmd;
		}

		String[] cmdPathArr = send(String.format("find / -name \"%s\"", cmdForSearch)).get("result").split("\n");
		for(String path: cmdPathArr){
			if (path.toLowerCase().contains("/bin/")){
				return cmd.replace(cmdForSearch, path);
			}		
		}
		cmdPaths.put(cmdForSearch, cmdPathArr[cmdPathArr.length - 1]);
		return cmd.replace(cmdForSearch, cmdPathArr[cmdPathArr.length - 1]);
	}

	public boolean isSshConected() throws CustomException {
		try {
			session = getSession();
		} catch (JSchException e) {
			throw new CustomException(e);
		}
		return session.isConnected();
	}


	private <K, V> Map<K, V> zipToMap(List<K> keys, List<V> values) {
		return IntStream.range(0, keys.size()).boxed()
				.collect(Collectors.toMap(keys::get, values::get));
	}

//	public void writeConsole(String val) {
//		Platform.runLater(() -> {
//			controller.writeConsole(val);
//		});
//	}

	@Override
	public String toString() {
		return "JSSHClient [host=" + host + ", user=" + user + ", password=" + password + "]";
	}


	private static class RunCmd extends Task<HashMap<String, String>> {
		private String cmd;

		public RunCmd(String cmd) {
			this.cmd = cmd;
		}

		@Override
		protected HashMap<String, String> call() throws Exception {

			return null;
		}
	}
}
