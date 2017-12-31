package bytecode;

import java.io.IOException;

public class Main {

	public static void main(String[] args) {
		JavaClass javaClass = new JavaClass("Main.class");
		try {
			javaClass.parseClassFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
