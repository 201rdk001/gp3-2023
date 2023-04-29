// 221RDB019 Mārtiņš Daugavietis 15.grupa
// 201RDK001 Kristaps Arnolds Kaidalovs 16.grupa
// 221RDB023 Keita Laimiņa 16.grupa

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

class CodeInputStream extends InputStream {
    private final InputStream in;
    private int codeBuf;
    private int bitsFree;
    private boolean eof;
    
    public int codeLength;
    
    public CodeInputStream(InputStream stream) {
        in = stream;
        codeLength = 9;
        bitsFree = Integer.SIZE;
        eof = false;
    }

    @Override
    public int read() throws IOException {
        int num = -1;
        
        do {
            num = in.read();
        
            if (num == -1)
                break;
                
            codeBuf = codeBuf | num << (bitsFree - 8);
            bitsFree -= 8;	
        } while (num != -1 && bitsFree >= 8);
            
        int value = codeBuf >>> (Integer.SIZE - codeLength);
        codeBuf = codeBuf << codeLength;
        bitsFree += codeLength;
        
        if (num == -1)
            eof = bitsFree >= Integer.SIZE - codeLength;
        
        return value;
    }
    
    public boolean eof() {
        return eof;
    }
}

class CodeOutputStream extends OutputStream {
    private final OutputStream out;
    private int codeBuf;
    private int bitsFree;
    
    public int codeLength;
    
    public CodeOutputStream(OutputStream stream) {
        this.out = stream;
        this.codeLength = 9;
        this.bitsFree = Integer.SIZE;
    }
    
    @Override
    public void write(int value) throws IOException {
        codeBuf = codeBuf | value << (bitsFree - codeLength);
        bitsFree -= codeLength;
        
        while (bitsFree <= Integer.SIZE - 8) {
            out.write(codeBuf >>> 24);
            codeBuf = codeBuf << 8;
            bitsFree += 8;
        }
    }
    
    public void flush() throws IOException {
        while (bitsFree < Integer.SIZE && codeBuf != 0) {
            out.write(codeBuf >>> 24);
            codeBuf = codeBuf << 8;
            bitsFree += 8;
        }
    }
}

class LZW {
    public static void compress(InputStream in, OutputStream out) throws IOException {
        Map<String, Integer> dictionary = new HashMap<>();
        CodeOutputStream bout = new CodeOutputStream(out);
        
        for (int i = 0; i < 256; i++)
            dictionary.put(Character.toString((char)i), i);
        
        String str = "";
        int byteIn;

        while ((byteIn = in.read()) != -1) {
            char sym = (char)byteIn;
            String strSym = str + sym;
            
            if (dictionary.containsKey(strSym)) {
                str = strSym;
            }
            else {
                bout.write(dictionary.get(str));
                dictionary.put(strSym, dictionary.size());
                str = Character.toString(sym);
            }
            
            if (dictionary.size() >= 1 << bout.codeLength)
                bout.codeLength++;
        }
        
        if (!str.equals(""))
            bout.write(dictionary.get(str));
        
        bout.flush();
    }
    
    public static void decompress(InputStream in, OutputStream out) throws IOException {
        List<String> dictionary = new ArrayList<String>(256);
        CodeInputStream bin = new CodeInputStream(in);
        DataOutputStream dout = new DataOutputStream(out);
        
        for (int i = 0; i < 256; i++)
            dictionary.add(Character.toString((char)i));

        String entry;
        char ch;
        int prevCode, currCode;
        
        prevCode = bin.read();
        entry = dictionary.get(prevCode);
        dout.writeBytes(entry);
        ch = entry.charAt(0);
        
        while (!bin.eof()) {
            currCode = bin.read();
            
            if (currCode >= dictionary.size()) {
                entry = dictionary.get(prevCode);
                entry += ch;
            }
            else {
                entry = dictionary.get(currCode);
            }
            
            dout.writeBytes(entry);
            ch = entry.charAt(0);
            dictionary.add(dictionary.get(prevCode)+ch);
            prevCode = currCode;
            
            if (dictionary.size() + 1 >= 1 << bin.codeLength)
                bin.codeLength++;
        }
    }
}

public class Main {
    public static Scanner sc = new Scanner(System.in);
    
    public static void main(String[] args) throws IOException {
        int izv = 0;
        do {
            System.out.println("\n1. compress");
            System.out.println("2. decompress");
            System.out.println("3. size");
            System.out.println("4. equal");
            System.out.println("5. about");
            System.out.println("6. exit\n");
            izv = sc.nextInt();
            switch(izv) {
            case 1:
                compress();
                break;
            case 2:
                decompress();
                break;
            case 3:
                size();
                break;
            case 4:
                equal();
                break;
            case 5:
                System.out.println("221RDB019 Mārtiņš Daugavietis 15.grupa");
                System.out.println("201RDK001 Kristaps Arnolds Kaidalovs 16.grupa");
                System.out.println("221RDB023 Keita Laimiņa 16.grupa");
                break;
            case 6:
                System.out.println("Compressor closed");
                break;
            default:
                System.out.println("Error, wrong number");
            break;
          }
        } while (izv != 6);
        sc.close();
        
    }
    
    public static void compress() {
        System.out.println("Source file name:");
        sc.nextLine();
        String inFilename=sc.nextLine();
        System.out.println("Archive name:");  
        String outFilename=sc.nextLine();  
        try {
            InputStream in;
            OutputStream out;

            in = new BufferedInputStream(new FileInputStream(inFilename));
            out = new BufferedOutputStream(new FileOutputStream(outFilename));
            LZW.compress(in, out);
            in.close();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void decompress() {
        System.out.println("Archive name:");
        sc.nextLine();
        String inFilename=sc.nextLine();
        System.out.println("Source file name:");  
        String outFilename=sc.nextLine();  
        try {
            InputStream in;
            OutputStream out;

            in = new BufferedInputStream(new FileInputStream(inFilename));
            out = new BufferedOutputStream(new FileOutputStream(outFilename));
            LZW.decompress(in, out);
            in.close();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void equal() throws IOException {
        System.out.println("First file name:");
        sc.nextLine();
        String inFilename1=sc.nextLine();
        System.out.println("Second file name:");
        String inFilename2=sc.nextLine();
        try {
            BufferedReader r1 = new BufferedReader(new FileReader(inFilename1));
            BufferedReader r2 = new BufferedReader(new FileReader(inFilename2));
            String s1 = r1.readLine();
            String s2 = r2.readLine();
            boolean same = true;
            while (s1 != null || s2 != null){
                if(s1 == null || s2 == null){
                    same = false;
                    break;
                }else if(! s1.equals(s2)) {
                    same = false; 
                       break;
                }
                s1 = r1.readLine();
                s2 = r2.readLine();
            } 
            r1.close();
            r2.close();
            System.out.println(same);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void size() {
        System.out.println("File name:");
        sc.nextLine();
        String Filename=sc.nextLine();
        try {
            File file = new File(Filename);
            long bytes = file.length();
            System.out.println("Size: " + bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}