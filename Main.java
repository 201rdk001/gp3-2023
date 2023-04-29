// 221RDB019 Mārtiņš Daugavietis 15.grupa
// 201RDK001 Kristaps Arnolds Kaidalovs 16.grupa
// 221RDB023 Keita Laimiņa 16.grupa

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.TreeMap;

//#region LZ77

class Sequence {
    // Note: matchOff is the "Cooked match offset", not the "Raw match offset"
    // For the difference, see https://nigeltao.github.io/blog/2022/zstandard-part-1-concepts.html#matches

    public int litLen;
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
    public int[] literalFrequencies;
    public List<Sequence> sequences;
}

class LZ77 {
    public int windowSize = 524288; // 512K, needs to be lowered later
    public int minMatchLen = 3;

    public LZ77Data compress(byte[] data) {
        LZ77Data result = new LZ77Data();
        List<Sequence> matches = new ArrayList<Sequence>();
        byte[] litBuf = new byte[data.length];
        int[] frequencies = new int[256];
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
                // Convert "Raw offset match" into "Cooked offset match"
                maxMatch += 3;

                matches.add(new Sequence(litLen, maxMatch, maxMatchLen));
                litLen = 0;
                i += (maxMatchLen - 1);
            }
            else {
                litBuf[litPos] = data[i];
                // Java bytes are always signed, and unsigned variants
                // don't exist, so conversion to to integer is required
                frequencies[(data[i] & 0xff)]++;
                litPos++;
                litLen++;
            }
        }

        matches.add(new Sequence(litLen, 3, 0));

        result.sequences = matches;
        result.literals = new byte[litPos];
        result.literalFrequencies = frequencies;
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
            int rawMatchOff = seq.matchOff - 3;
            byteCopy(data, rawMatchOff, seq.matchLen, data, dataPos);
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
//#endregion

//#region Huffman

class HuffmanNode implements Comparable<HuffmanNode> {
    public int weight;
    public int symbol;
    public int code;
    public HuffmanNode parent;
    public HuffmanNode left;
    public HuffmanNode right;
    public TreeMap<Integer, HuffmanNode> leafs;

    public HuffmanNode(int weight, HuffmanNode left, HuffmanNode right) {
        this.weight = weight;
        this.left = left;
        this.right = right;
    }

    public HuffmanNode(int weight, int symbol) {
        this.weight = weight;
        this.symbol = symbol;
    }

    @Override
    public int compareTo(HuffmanNode node) {
        return Integer.compare(this.weight, node.weight);
    }
}

class Huffman {
    public HuffmanNode generateHuffmanTree(int[] symbolFrequencies) {
        // Generate all leaf nodes
        TreeMap<Integer, HuffmanNode> leafs = new TreeMap<Integer, HuffmanNode>();
        for (int i = 0; i < symbolFrequencies.length; i++) {
            if (symbolFrequencies[i] > 0) {
                leafs.put(i, new HuffmanNode(symbolFrequencies[i], i));
            }
        }
        
        // Create sorted set for all nodes of the tree, initialize with leafs
        PriorityQueue<HuffmanNode> nodes =
            new PriorityQueue<HuffmanNode>(leafs.values());

        // Construct Huffman tree
        HuffmanNode newParent = new HuffmanNode(0, 0);
        while (nodes.size() > 1) {
            HuffmanNode left = nodes.poll();
            HuffmanNode right = nodes.poll();

            if (left == null || right == null)
                System.out.println();
            
            newParent = new HuffmanNode(
                left.weight + right.weight,
                left,
                right
            );

            left.parent = newParent;
            right.parent = newParent;
            nodes.add(newParent);
        }

        // Generate prefix codes for each symbol
        for (HuffmanNode leaf : leafs.values()) {
            HuffmanNode previousNode, currentNode = leaf;
            while (currentNode.parent != null) {
                previousNode = currentNode;
                currentNode = previousNode.parent;

                leaf.code = leaf.code << 1;
                if (currentNode.right == previousNode) {
                    leaf.code += 1;
                }
            }
        }

        newParent.leafs = leafs;
        return newParent;
    }

    public byte[] compress(byte[] data, HuffmanNode huffmanTree) {
        byte[] buf = new byte[data.length];
        BitStreamWriter writer = new BitStreamWriter(buf);

        for (int i = 0; i < data.length; i++) {
            writer.write(huffmanTree.leafs.get((int)data[i]).code);
        }

        return Arrays.copyOfRange(buf, buf.length - writer.length() + 1, buf.length);
    }

    public byte[] decompress(int origSize, byte[] compressed, byte[] symbolMap) {
        return new byte[0];
    }
}
//#endregion

class Zstd {
    public static void compress(String firstFile, String secondFile) {
        return;
    }

    public static void decompress(String firstFile, String secondFile) {
        return;
    }
}

public class Main {
    static Scanner sc = new Scanner(System.in);
    static String command;
    static String firstFile, secondFile;

    public static void main(String[] args) {
        System.out.println("started");
        prompt: while (true) {
            command = sc.next();
            switch (command) {
                case "comp": {
                    compress();
                    break;
                }
                case "decomp": {decompress(); break;}
                case "equal": {equal(); break;}
                case "size": {size(); break;}
                case "about": {about(); break;}
                case "debug": { Debug.test(); break;}
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

//#region Utility

class BitStreamReader {
    private int bitBuf;
    private int bitsUsed;
    private int pos;
    private int len;
    public byte[] buf;
    
    public BitStreamReader(byte[] buf, int len) {
        this.pos = buf.length-1;
        this.buf = buf;
        this.len = len;
    }

    public int readBit() {
        return read(1);
    }

    public int read(int bitSize) {
        while (bitsUsed < bitSize) {
            if (pos < buf.length - len) {
                break;
            }

            bitBuf = bitBuf | buf[pos] << bitsUsed;
            bitsUsed += 8;
            pos--;

            // Sentinel bit
            if (pos < buf.length - len) {
                int highBit = Integer.highestOneBit(bitBuf);
                bitsUsed = Integer.numberOfTrailingZeros(highBit);
                bitBuf ^= highBit;
            }
        }

        int value = bitBuf & ~(0xFF << bitSize);
        bitBuf = bitBuf >> bitSize;
        bitsUsed -= bitSize;

        return value;
    }

    public boolean endOfStream() {
        return (pos < buf.length - len) && bitsUsed == 0;
    }
}

class BitStreamWriter {
    private int bitBuf;
    private int bitsUsed;
    private int pos;
    public byte[] buf;
    
    public BitStreamWriter(byte[] buf) {
        this.pos = buf.length-1;
        this.buf = buf;
    }

    public void write(int value) {
        int bitSize = 1;
        if (value != 0) {
            bitSize = Integer.numberOfTrailingZeros(
                Integer.highestOneBit(value)
            ) + 1;
        }

        bitBuf = bitBuf | value << bitsUsed;
        bitsUsed += bitSize;
        
        while (bitsUsed >= 8) {
            buf[pos] = (byte)(bitBuf);
            pos--;
            bitBuf = bitBuf >>> 8;
            bitsUsed -= 8;
        }
    }

    public void flush() {
        // Sentinel bit
        write(1);

        while (bitBuf != 0) {
            buf[pos] = (byte)(bitBuf);
            pos--;
            bitBuf = bitBuf >>> 8;
            bitsUsed -= 8;
        }

        bitsUsed = 0;
    }

    public int length() {
        return buf.length - pos;
    }
}

// TODO: remove later
class Debug {
    public static void test() {
        System.out.println("LZ77");
        LZ77 LZComp = new LZ77();
        Huffman HuffComp = new Huffman();
        byte[] bitStream = new byte[10];
        debugHuff(
            HuffComp,
            debugLZ(LZComp, readFromFile("data/romeo.txt"), 1),
            1
        );
        //debugLZ(LZComp, readFromFile("data/negativeBytes.bin"), 2);
        //debugLZ(LZComp, readFromFile("data/File2.html"), 3);

        BitStreamWriter writer = new BitStreamWriter(bitStream);
        BitStreamReader reader = new BitStreamReader(bitStream, writer.length());
        debugBitWriter(writer);
        debugBitReader(reader);
    }

    public static void debugBitWriter(BitStreamWriter writer) {
        writer.write(0);
        writer.write(1);
        writer.write(2);
        writer.write(3);
        writer.flush();
    }

    public static void debugBitReader(BitStreamReader reader) {
        System.out.println(reader.readBit());
        System.out.println(reader.readBit());
        System.out.println(reader.endOfStream());
        System.out.println(reader.read(2));
        System.out.println(reader.read(2));
        System.out.println(reader.endOfStream());
        System.out.println(reader.read(8));
    }

    public static void debugHuff(Huffman comp, LZ77Data data, int testNo) {
        System.out.printf("Huff Comp %d\n", testNo);
        System.out.print("Huffman bitstream: ");
        writeToFile(comp.compress(
            data.literals,
            comp.generateHuffmanTree(data.literalFrequencies)
        ), testNo + ".huff");
    }

    public static LZ77Data debugLZ(LZ77 comp, byte[] inData, int testNo) {
        System.out.printf("LZ Comp %d\n", testNo);
        LZ77Data compData = comp.compress(inData);

        System.out.print("Data: ");
        //printByteArray(inData);
        writeToFile(inData, testNo + ".orig.bin");
        
        System.out.print("Literals: ");
        //printByteArray(compData.literals);
        writeToFile(compData.literals, testNo + ".lit");

        System.out.print("Frequencies: ");
        //printIntArray(compData.literalFrequencies);
        
        System.out.print("Sequences: ");
        //printSequences(compData.sequences);
        System.out.println(compData.sequences.size());

        System.out.print("Decomp: ");
        byte[] outData = comp.decompress(compData.sequences, compData.literals);
        //printByteArray(outData);
        writeToFile(outData, testNo + ".bin");

        return compData;
    }

    public static void printSequences(List<Sequence> sequences) {
        for (Sequence seq : sequences) {
            System.out.printf(
                "(%d, %d, %d), ",
                seq.litLen,
                seq.matchOff,
                seq.matchLen
            );
        }
        System.out.println();
    }

    public static void printByteArray(byte[] data) {
        System.out.print("{ ");
        for (int i = 0; i < data.length; i++) {
            System.out.printf("%02X ", data[i]);
        }
        System.out.println("}");
    }

    public static void printIntArray(int[] data) {
        System.out.print("{ ");
        for (int i = 0; i < data.length; i++) {
            System.out.printf("%02X ", data[i]);
        }
        System.out.println("}");
    }

    public static void writeToFile(byte[] data, String filename) {
        try {
            FileOutputStream f = new FileOutputStream(filename);
            f.write(data);
            f.close();
        } catch (Exception e) {
            System.out.println("writeToFile failed!");
        }
    }

    public static byte[] readFromFile(String filename) {
        try {
            FileInputStream f = new FileInputStream(filename);
            byte[] ret = f.readAllBytes();
            f.close();
            return ret;
        } catch (Exception e) {
            System.out.println("readFromFile failed!");
            return new byte[0];
        }
    }
}
//#endregion