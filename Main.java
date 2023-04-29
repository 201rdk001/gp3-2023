// 221RDB019 Mārtiņš Daugavietis 15.grupa
// 201RDK001 Kristaps Arnolds Kaidalovs 16.grupa
// 221RDB023 Keita Laimiņa 16.grupa

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Scanner;

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

