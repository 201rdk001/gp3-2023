// 221RDB019 Mārtiņš Daugavietis 15.grupa
// 201RDK001 Kristaps Arnolds Kaidalovs 16.grupa
// 221RDB023 Keita Laimiņa 16.grupa

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.HashMap;

class BitStreamInput {
    private InputStream stream;
    private int buf;
    private int usedBits;
    public int bitLen;

    public BitStreamInput(InputStream stream) {
        this.stream = stream;
        this.bitLen = 9;
        this.usedBits = Integer.SIZE;
    }

    public int read() throws IOException {
        int byteIn;

        do {
            if ((byteIn = stream.read()) == -1)
                break;

            buf = buf | byteIn << (usedBits - 8);
            usedBits -= 8;
        } while (byteIn != -1 && usedBits >= 8);

        int value = buf >>> (Integer.SIZE - bitLen);
        buf = buf << bitLen;
        usedBits += bitLen;

        return value;
    }

    public boolean eof() {
        return usedBits >= Integer.SIZE - bitLen;
    }
}

class BitStreamOutput {
    private OutputStream stream;
    private int buf;
    private int usedBits;
    public int bitLen;

    public BitStreamOutput(OutputStream stream) {
        this.stream = stream;
        this.bitLen = 9;
        this.usedBits = Integer.SIZE;
    }

    public void write(int value) throws IOException {
        buf = buf | value << (usedBits - bitLen);
        usedBits -= bitLen;

        while (usedBits <= Integer.SIZE - 8) {
            stream.write(buf >>> 24);
            buf = buf << 8;
            usedBits += 8;
        }
    }

    public void flush() throws IOException {
        while (usedBits < Integer.SIZE && buf != 0) {
            stream.write(buf >>> 24);
            buf = buf << 8;
            usedBits += 8;
        }
    }
}

class LZW {
    public static void compress(FileInputStream in, BitStreamOutput binaryOut) throws IOException {
        HashMap<String, Integer> dict = new HashMap<String, Integer>();

        for (int i = 0; i < (1 << Byte.SIZE); i++)
            dict.put(Character.toString((char)i), i);

        String currStr = "";
        int byteIn;

        while ((byteIn = in.read()) != -1) {
            char sym = (char)byteIn;
            String currStrSymbol = currStr + sym;

            if (dict.containsKey(currStrSymbol)) {
                currStr = currStrSymbol;
            }
            else {
                binaryOut.write(dict.get(currStr));
                dict.put(currStrSymbol, dict.size());
                currStr = Character.toString(sym);
            }

            if (dict.size() >= 1 << binaryOut.bitLen)
                binaryOut.bitLen++;
        }

        if (!currStr.equals(""))
            binaryOut.write(dict.get(currStr));

        binaryOut.flush();
    }

    public static void decompress(BitStreamInput binaryIn, DataOutputStream dataOut) throws IOException {
        ArrayList<String> dict = new ArrayList<String>((1 << Byte.SIZE));

        for (int i = 0; i < (1 << Byte.SIZE); i++)
            dict.add(Character.toString((char)i));

        String str;
        char ch;
        int prevIdx, currIdx;

        prevIdx = binaryIn.read();
        str = dict.get(prevIdx);
        dataOut.writeBytes(str);
        ch = str.charAt(0);

        while (!binaryIn.eof()) {
            currIdx = binaryIn.read();

            if (currIdx >= dict.size()) {
                str = dict.get(prevIdx);
                str += ch;
            }
            else {
                str = dict.get(currIdx);
            }

            dataOut.writeBytes(str);
            ch = str.charAt(0);
            dict.add(dict.get(prevIdx)+ch);
            prevIdx = currIdx;

            if (dict.size() + 1 >= 1 << binaryIn.bitLen)
                binaryIn.bitLen++;
        }
    }
}

public class Main {
    static Scanner sc = new Scanner(System.in);
    public static void main(String[] args) throws IOException {
        String command;

        do {
            System.out.print("command>");
            command = sc.nextLine();
            switch (command) {
                case "comp": compress(); break;
                case "decomp": decompress(); break;
                case "equal": equal(); break;
                case "size": size(); break;
                case "about": about(); break;
                case "exit": break;
                default: System.out.println("unknown command");
          }
        } while (!command.equals("exit"));

        sc.close();
    }

    public static void compress() {
        System.out.println("source file name:");
        String source=sc.nextLine();
        System.out.println("archive name:");
        String archive=sc.nextLine();

        try {
            FileInputStream in = new FileInputStream(source);
            FileOutputStream out = new FileOutputStream(archive);
            BitStreamOutput bout = new BitStreamOutput(out);

            LZW.compress(in, bout);

            in.close();
            out.close();
        } catch (Exception ex) {
            System.out.println("Error opening or reading file, check input");
            System.out.println(ex.getMessage());
        }
    }

    public static void decompress() {
        System.out.println("archive name:");
        String archive=sc.nextLine();
        System.out.println("file name:");
        String source=sc.nextLine();

        try {
            FileInputStream in = new FileInputStream(archive);
            BitStreamInput bin = new BitStreamInput(in);
            FileOutputStream out = new FileOutputStream(source);
            DataOutputStream dout = new DataOutputStream(out);

            LZW.decompress(bin, dout);

            in.close();
            out.close();
        } catch (Exception ex) {
            System.out.println("Error opening or reading file, check input");
            System.out.println(ex.getMessage());
        }
    }

    public static void equal() throws IOException {
        System.out.print("first file name: ");
        String file1 = sc.nextLine();
        System.out.print("second file name: ");
        String file2 = sc.nextLine();
        try {
            FileInputStream f1 = new FileInputStream(file1);
            FileInputStream f2 = new FileInputStream(file2);
            int size1, size2;
            byte[] buf1 = new byte[1000];
            byte[] buf2 = new byte[1000];
            do {
                size1 = f1.read(buf1);
                size2 = f2.read(buf2);
                if (size1 != size2) {
                    f1.close();
                    f2.close();
                    System.out.println(false);
                    return;
                }

                for (int i=0; i < size1; i++) {
                    if (buf1[i] != buf2[i]) {
                        f1.close();
                        f2.close();
                        System.out.println(false);
                        return;
                }
            }
            } while (size1 == 0 && size2 == 0);

            f1.close();
            f2.close();
            System.out.println(true);
            return;
        } catch (IOException ex) {
            System.out.println("Error opening or reading file, check input");
            System.out.println(ex.getMessage());
            System.out.println(false);
        }
    }

    public static void size() {
        System.out.println("file name");
        String Filename = sc.nextLine();
        try {
            File file = new File(Filename);
            System.out.println("size: " + file.length());
        } catch (Exception ex) {
            System.out.println("Error opening or reading file, check input");
            System.out.println(ex.getMessage());
        }
    }

    public static void about() {
        System.out.println("221RDB019 Mārtiņš Daugavietis 15.grupa");
        System.out.println("201RDK001 Kristaps Arnolds Kaidalovs 16.grupa");
        System.out.println("221RDB023 Keita Laimiņa 16.grupa");
    }
}