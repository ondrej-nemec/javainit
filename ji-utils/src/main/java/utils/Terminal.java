package utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

import common.Logger;
import common.OperationSystem;
import text.BufferedReaderFactory;
import text.plaintext.PlainTextLoader;

public class Terminal {
	
	private final Logger logger;
	
	public Terminal(final Logger logger) {
		this.logger = logger;
	}
	
	public int runFile(final Consumer<String> stdOut, final Consumer<String> stdErr, final String fileName) {
		return run(stdOut, stdErr, fileName + OperationSystem.CLI_EXTENSION);
	}
	
	/**
	 * Asynchronious
	 * @param command
	 */
	public void runFile(final String fileName) {
		run(fileName + OperationSystem.CLI_EXTENSION);
	}
	
	public int runCommand(final Consumer<String> stdOut, final Consumer<String> stdErr, final String command) {
		return run(stdOut, stdErr, OperationSystem.PRE_COMMAND + command);
	}
	
	/**
	 * Asynchronious
	 * @param command
	 */
	public void runCommand(final String command) {
		run(OperationSystem.PRE_COMMAND + command);
	}
	
	private void run(final String command) {
		try {
			Runtime.getRuntime().exec(command);
			logger.debug("Command: " + command + " was started");
		}catch (IOException e) {
			logger.error("Command could not run: " + command, e);
		}
	}
	
	private int run(final Consumer<String> stdOut, final Consumer<String> stdErr, final String command) {
		try {
			Process pr = Runtime.getRuntime().exec(command);
			pr.waitFor();
			
			readsAndApplyConsumer(pr.getInputStream(), stdOut);
			readsAndApplyConsumer(pr.getErrorStream(), stdErr);
			
			int exitValue = pr.exitValue();
			logger.debug("Command: " + command + " will return: " + exitValue);
			return exitValue;
		} catch (IOException | InterruptedException e) {
			logger.error("Command could not run: " + command, e);
		}
		return -1;
	}
	
	private void readsAndApplyConsumer(final InputStream stream, final Consumer<String> consumer) throws IOException {
		try (BufferedReader br = BufferedReaderFactory.buffer(stream)) {
			new PlainTextLoader(br).read(consumer);
		} finally {}
	}
}
