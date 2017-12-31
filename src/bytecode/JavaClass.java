package bytecode;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class JavaClass {

	private final String[] FMT = { null, "Utf8", null, "Integer", "Float", "Long", "Double", "Class", "String", "Field", "Method", "Interface Method", "Name and Type", null, null, "MethodHandle", "MethodType", null, "InvokeDynamic" };
	public File file;

	public JavaClass(String string) {
		this.file = new File(string);
	}

	public void parseRtClass(Class<?> clazz) throws IOException, URISyntaxException {
		URL url = clazz.getResource(clazz.getSimpleName() + ".class");
		if (url == null)
			throw new IOException("can't access bytecode of " + clazz);
		parse(ByteBuffer.wrap(Files.readAllBytes(Paths.get(url.toURI()))));
	}

	public String parseClassFile() throws IOException {
		Path path = Paths.get(file.toURI());
		ByteBuffer bb;
		FileChannel ch = FileChannel.open(path, StandardOpenOption.READ);
		bb = ch.map(FileChannel.MapMode.READ_ONLY, 0, ch.size());
		parse(bb);
		return null;
	}

	public void parse(ByteBuffer buf) {
		if (buf.order(ByteOrder.BIG_ENDIAN).getInt() != ConstantPool.HEAD) {
			System.out.println("not a valid class file");
			return;
		}
		int minor = buf.getChar(), ver = buf.getChar();
		System.out.println("version " + ver + '.' + minor);
		for (int ix = 1, num = buf.getChar(); ix < num; ix++) {
			String s;
			int index1 = -1, index2 = -1;
			byte tag = buf.get();
			switch (tag) {
			default:
				System.out.println("unknown pool item type " + buf.get(buf.position() - 1));
				return;
			case ConstantPool.CONSTANT_Utf8:
				decodeString(ix, buf);
				continue;
			case ConstantPool.CONSTANT_Class:
			case ConstantPool.CONSTANT_String:
			case ConstantPool.CONSTANT_MethodType:
				s = "%d:\t%s ref=%d%n";
				index1 = buf.getChar();
				break;
			case ConstantPool.CONSTANT_FieldRef:
			case ConstantPool.CONSTANT_MethodRef:
			case ConstantPool.CONSTANT_InterfaceMethodRef:
			case ConstantPool.CONSTANT_NameAndType:
				s = "%d:\t%s ref1=%d, ref2=%d%n";
				index1 = buf.getChar();
				index2 = buf.getChar();
				break;
			case ConstantPool.CONSTANT_Integer:
				s = "%d:\t%s value=" + buf.getInt() + "%n";
				break;
			case ConstantPool.CONSTANT_Float:
				s = "%d:\t%s value=" + buf.getFloat() + "%n";
				break;
			case ConstantPool.CONSTANT_Double:
				s = "%d:\t%s value=" + buf.getDouble() + "%n";
				ix++;
				break;
			case ConstantPool.CONSTANT_Long:
				s = "%d:\t%s value=" + buf.getLong() + "%n";
				ix++;
				break;
			case ConstantPool.CONSTANT_MethodHandle:
				s = "%d:\t%s kind=%d, ref=%d%n";
				index1 = buf.get();
				index2 = buf.getChar();
				break;
			case ConstantPool.CONSTANT_InvokeDynamic:
				s = "%d:\t%s bootstrap_method_attr_index=%d, ref=%d%n";
				index1 = buf.getChar();
				index2 = buf.getChar();
				break;
			}
			System.out.printf(s, ix, FMT[tag], index1, index2);
		}
	}

	private void decodeString(int poolIndex, ByteBuffer buf) {
		int size = buf.getChar(), oldLimit = buf.limit();
		buf.limit(buf.position() + size);
		StringBuilder sb = new StringBuilder(size + (size >> 1) + 16).append(poolIndex).append(":\tUtf8 ");
		while (buf.hasRemaining()) {
			byte b = buf.get();
			if (b > 0)
				sb.append((char) b);
			else {
				int b2 = buf.get();
				if ((b & 0xf0) != 0xe0)
					sb.append((char) ((b & 0x1F) << 6 | b2 & 0x3F));
				else {
					int b3 = buf.get();
					sb.append((char) ((b & 0x0F) << 12 | (b2 & 0x3F) << 6 | b3 & 0x3F));
				}
			}
		}
		buf.limit(oldLimit);
		System.out.println(sb);
	}

}
