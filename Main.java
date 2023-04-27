// 221RDB019 Mārtiņš Daugavietis 15.grupa
// 201RDK001 Kristaps Arnolds Kaidalovs 16.grupa
// 221RDB023 Keita Laimiņa 16.grupa

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

class Sequence {
    public int litLen;
    // TODO: do we store "raw" or "cooked" offset?
    // For the difference, see https://nigeltao.github.io/blog/2022/zstandard-part-1-concepts.html#matches
    public int matchOff;
    public int matchLen;

    public Sequence(int litLen, int matchOff, int matchLen) {
        this.litLen = litLen;
        this.matchOff = matchOff;
        this.matchLen = matchLen;
    }
}

class LZ77Data {
    public byte[] literals;
    public List<Sequence> sequences;
}

class LZ77 {
    public int windowSize = 4096;
    public int minMatchLen = 3;

    public LZ77Data compress(byte[] data) {
        LZ77Data result = new LZ77Data();
        List<Sequence> matches = new ArrayList<Sequence>();
        byte[] litBuf = new byte[data.length];
        int litPos = 0;
        int litLen = 0;

        for (int i = 0; i < data.length; i++) {
            int maxMatch = -1;
            int maxMatchLen = 0;
            int searchBufPos = Math.max(0, i - windowSize);
            
            for (int j = searchBufPos; j < i; j++) {
                int curMatch = j;
                int curMatchLen = 0;
                int off = 0;
                
                while (data[j+off] == data[i+off]) {
                    curMatchLen++;
                    off++;

                    if (i + off >= data.length) {
                        break;
                    }
                }

                if (curMatchLen > maxMatchLen) {
                    maxMatch = curMatch;
                    maxMatchLen = curMatchLen;
                }  
            }

            if (maxMatch >= 0 && maxMatchLen >= minMatchLen) {
                matches.add(new Sequence(litLen, maxMatch, maxMatchLen));
                litLen = 0;
                i += (maxMatchLen - 1);
            }
            else {
                litBuf[litPos] = data[i];
                litPos++;
                litLen++;
            }
        }

        matches.add(new Sequence(litLen, 0, 0));

        result.sequences = matches;
        result.literals = new byte[litPos];
        byteCopy(litBuf, 0, litPos, result.literals, 0);
        return result;
    }

    public byte[] decompress(List<Sequence> sequences, byte[] literals) {
        byte[] data = new byte[calcBufSize(sequences, false)];
        int litPos = 0;
        int dataPos = 0;

        for (Sequence seq : sequences) {
            // Literal copy
            byteCopy(literals, litPos, seq.litLen, data, dataPos);
            litPos += seq.litLen;
            dataPos += seq.litLen;

            // Match copy
            byteCopy(data, seq.matchOff, seq.matchLen, data, dataPos);
            dataPos += seq.matchLen;
        }

        return data;
    }

    private int calcBufSize(List<Sequence> sequences, boolean litOnly) {
        int size = 0;
        for (Sequence seq : sequences) {
            size += seq.litLen + (litOnly ? 0 : seq.matchLen);
        }
        return size;
    }

    private void byteCopy(byte[] src, int srcPos, int srcLen, byte[] dst, int dstPos) {
        for (int i = 0; i < Math.min(srcLen, dst.length); i++) {
            dst[i+dstPos] = src[i+srcPos];
        }
    }
}

class Zstd {
    public static void compress(String firstFile, String secondFile) {
        return;
    }

    public static void decompress(String firstFile, String secondFile) {
        return;
    }
}

// TODO: remove later
class Debug {
    public static String romeo = "Romeo and Juliet\n" +
    "Excerpt from Act 2, Scene 2\n" +
    "\n" +
    "JULIET\n" +
    "O Romeo, Romeo! wherefore art thou Romeo?\n" +
    "Deny thy father and refuse thy name;\n" +
    "Or, if thou wilt not, be but sworn my love,\n" +
    "And I'll no longer be a Capulet.\n" +
    "\n" +
    "ROMEO\n" +
    "[Aside] Shall I hear more, or shall I speak at this?\n" +
    "\n" +
    "JULIET\n" +
    "'Tis but thy name that is my enemy;\n" +
    "Thou art thyself, though not a Montague.\n" +
    "What's Montague? it is nor hand, nor foot,\n" +
    "Nor arm, nor face, nor any other part\n" +
    "Belonging to a man. O, be some other name!\n" +
    "What's in a name? that which we call a rose\n" +
    "By any other name would smell as sweet;\n" +
    "So Romeo would, were he not Romeo call'd,\n" +
    "Retain that dear perfection which he owes\n" +
    "Without that title. Romeo, doff thy name,\n" +
    "And for that name which is no part of thee\n" +
    "Take all myself.\n" +
    "\n" +
    "ROMEO\n" +
    "I take thee at thy word:\n" +
    "Call me but love, and I'll be new baptized;\n" +
    "Henceforth I never will be Romeo.\n" +
    "\n" +
    "JULIET\n" +
    "What man art thou that thus bescreen'd in night\n" +
    "So stumblest on my counsel?\n";

    public static void writeToFile(byte[] data, String filename) {
        try {
            FileOutputStream f = new FileOutputStream(filename);
            f.write(data);
            f.close();
        } catch (Exception e) {
            System.out.println("writeToFile failed!");
        }
    }

    public static void test() {
        System.out.println("LZ77");
        LZ77 LZComp = new LZ77();
        // debugLZ(LZComp, new byte[] { 0, 0, 0, 0, 0, 0, 0, 0 }, 1);
        // debugLZ(LZComp, new byte[] { 0, 1, 0, 1, 0, 1, 0, 1 }, 2);
        // debugLZ(LZComp, new byte[] { 0, 1, 0, 1, 0, 1, 0, 1, 0 }, 3);
        // debugLZ(LZComp, new byte[] { 0, 1, 0, 1, 0, 1, 0, 1, 0, 1 }, 4);
        // debugLZ(LZComp, new byte[] { 0, 0, 1, 0, 0, 1, 2, 0, 0, 1, 2 }, 5);
        //debugLZ(LZComp, "Testing Some Text, Testing Text So Some".getBytes(StandardCharsets.ISO_8859_1), 6);
        debugLZ(LZComp, romeo.getBytes(StandardCharsets.ISO_8859_1), 7);
    }

    public static void debugLZ(LZ77 comp, byte[] inData, int testNo) {
        System.out.printf("Comp %d\n", testNo);
        LZ77Data compData = comp.compress(inData);

        System.out.print("Data: ");
        // System.out.print("{ ");
        // for (int i = 0; i < inData.length; i++) {
        //     System.out.printf("%02X ", inData[i]);
        // }
        // System.out.println("}");
        writeToFile(inData, testNo + ".orig.bin");
        
        
        System.out.print("Literals: ");
        System.out.print("{ ");
        for (int i = 0; i < compData.literals.length; i++) {
            System.out.printf("%02X ", compData.literals[i]);
        }
        System.out.println("}");
        writeToFile(compData.literals, testNo + ".lit");
        
        System.out.print("Sequences: ");
        for (Sequence seq : compData.sequences) {
            System.out.printf("(%d, %d, %d), ", seq.litLen, seq.matchOff, seq.matchLen);
        }
        System.out.println();

        System.out.print("Decomp: ");
        byte[] outData = comp.decompress(compData.sequences, compData.literals);
        // System.out.print("{ ");
        // for (int i = 0; i < outData.length; i++) {
        //     System.out.printf("%02X ", outData[i]);
        // }
        // System.out.println("}");
        writeToFile(outData, testNo + ".bin");
    }
}

public class Main {
    static Scanner sc = new Scanner(System.in);
    static String command;
    static String firstFile, secondFile;

    public static void main(String[] args) {
        prompt: while (true) {
            command = sc.next();
            switch (command) {
                case "comp": compress();
                case "decomp": decompress();
                case "equal": equal();
                case "size": size();
                case "about": about();
                case "debug": Debug.test();
                case "exit": {break prompt;}
                default: System.out.println("unknown command");
            }
        }
        sc.close();
    }

    public static void compress() {
        System.out.print("source file name: ");
        firstFile = sc.next();
        System.out.print("archive name: ");
        secondFile = sc.next();
        Zstd.compress(firstFile, secondFile);
    }

    public static void decompress() {
        System.out.print("archive name: ");
        firstFile = sc.next();
        System.out.print("file name: ");
        secondFile = sc.next();
        Zstd.decompress(firstFile, secondFile);
    }

    public static void equal() {
        System.out.print("first file name: ");
        firstFile = sc.next();
        System.out.print("second file name: ");
        secondFile = sc.next();
        boolean equal;
        try {
            FileInputStream f1 = new FileInputStream(firstFile);
            FileInputStream f2 = new FileInputStream(secondFile);
            int k1, k2;
            byte[] buf1 = new byte[1000];
            byte[] buf2 = new byte[1000];
            do {
                k1 = f1.read(buf1);
                k2 = f2.read(buf2);
                if (k1 != k2) {
                    f1.close();
                    f2.close();
                    System.out.println(false);
                    return;
                }
                for (int i=0; i<k1; i++) {
                    if (buf1[i] != buf2[i]) {
                        f1.close();
                        f2.close();
                        System.out.println(false);
                        return;
                    }
                }
            } while (k1 == 0 && k2 == 0);
            f1.close();
            f2.close();
            System.out.println(true);
            return;
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
            System.out.println(false);
        }
    }

    public static void size() {
        System.out.print("file name: ");
        firstFile = sc.next();
        try {
            FileInputStream f = new FileInputStream(firstFile);
            System.out.println("size: " + f.available());
            f.close();
        }
        catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }
    public static void about() {
        System.out.println("221RDB019 Mārtiņš Daugavietis 15.grupa");
        System.out.println("201RDK001 Kristaps Arnolds Kaidalovs 16.grupa");
        System.out.println("221RDB023 Keita Laimiņa 16.grupa");
    }
}

