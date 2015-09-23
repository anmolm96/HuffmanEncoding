import java.util.*;
import java.io.*;

public class HuffmanCodes2
{
  static Map<Byte, Integer> FreqChar;
  public  static final Map<String, String> escapedLiterals = new HashMap<>();
  static boolean encode = false, decode = false;
  static Node root;
  static int header = 0;
  static Map<Byte, String> codes;
  static String encodedMessage;
  static BitInputStream bi = null;
  static BitOutputStream bo = null;
  static PrintWriter pw = null;
  static byte[] text;
  static ArrayList<Byte> decodedMessage = new ArrayList<>();
  static char[] encodedSequence;
  static int length;

  public static void escapeLiterals()
  {
    escapedLiterals.put("\n", "\'\\n\'");
    escapedLiterals.put("\r", "\'\\r\'");
    escapedLiterals.put("\r", "\'\\r\'");
    escapedLiterals.put("\\", "\'\\\\\'");
    escapedLiterals.put("\'", "\'\\\'\'");
  }

  private static String asASCIILiteral(byte asciiValue) {
    String convertedStr =new String(new byte[] {asciiValue});
    String literal = escapedLiterals.get(convertedStr);
    if(literal == null) {
      literal = String.format("\'%s\'", convertedStr);
    }
    return literal;
  }
  private static class Node implements Comparable<Node>
  {
    Byte b;
    int freq;
    Node left, right;

    Node(Byte b, int freq, Node left, Node right)
    {
      this.b = b;
      this.freq = freq;
      this.left = left;
      this.right = right;
    }
    public int compareTo(Node n)
    {
      return this.freq - n.freq;
    }
  }
  public static void expand() throws IOException
  {
    //Do stuff to decode the string from binary to text
    length = bi.readInt();
    int zerocounter = 0, onecounter = 0;
    boolean vl = false, vr = false;
    int counter = 0;
    //decodedMessage = new byte[length];
    codes = new HashMap<Byte, String>();
    ArrayList<Character> ch = new ArrayList<>();
    while(onecounter != (zerocounter+1))
    {
      int a;
      int b = 0;
      do
      {
          a = bi.readBit();
          if(a == 0)
            ++zerocounter;

          if(vl == false)
          {
            //read left
            if(a==1)
            {
              vl = true;
            }
            if(a == 0)
            {
              vr = false;
            }
            ch.add('0');
            b = 0;
          }
          else if(vl == true)
          {
            //read
            if(a==0)
            {
              vl = false;
            }
            if(vr == true)
            {
              ch.remove(ch.size()-1);
            }
            if(a==1)
            {
              vr = true;
            }
            b = 1;
            ch.remove(ch.size()-1);
            ch.add('1');
          }
      }
      while(a != 1);
      ++onecounter;
      String s = "";
      for(int i = 1; i < ch.size(); i++)
      {
        s +=ch.get(i);
      }
      byte by = (byte)bi.readByte();
      decodedMessage.add(by);
      codes.put(by, s);
    }
    boolean loop = false;
    encodedSequence = new char[length];
    for(int i = 0; i < length; i++)
    {
      String s = "";
        do
          {
            s += bi.readBit();
            for(Map.Entry<Byte, String> en: codes.entrySet())
            {
              if(en.getValue().equals(s))
              {
                Byte[] temp = {en.getKey()};
                int q = temp[0].intValue();
                char c = Character.toChars(q)[0];
                encodedSequence[i] = c;
                loop = true;
              }
            }
          }
         while (loop == false);
         loop = false;
    }
    for(char c:encodedSequence)
    {
      pw.append(c);
    }
    pw.close();
  }
  public static void compress(byte[] sequence)
  {
    if(sequence == null)
      throw new NullPointerException();
    if(sequence.equals(""))
      throw new IllegalArgumentException();
    FreqChar = getCharacterFrequency(sequence);
    root = buildTree(FreqChar);
    codes = getCharacterCodes(FreqChar.keySet(), root);
    encodedMessage = getEncodedMessage(codes, sequence);
  }
  public static Map<Byte, Integer> getCharacterFrequency(byte[] s)
  {
    final Map<Byte, Integer> map = new HashMap<Byte, Integer>();
    for(int i = 0; i < s.length; i++)
    {
      Byte b = s[i];
      if(!map.containsKey(b))
        map.put(b, 1);
      else
      {
        map.put(b, map.get(b)+1);
      }
    }
    return map;
  }
  public static Node buildTree(Map<Byte, Integer> map)
  {
    Queue<Node> pq = new PriorityQueue<Node>();
    //Source: http://stackoverflow.com/questions/1066589/iterate-through-a-hashmap
    for(Map.Entry<Byte, Integer> entry : map.entrySet())
    {
      pq.add(new Node(entry.getKey(), entry.getValue(), null, null));
    }
    while(pq.size()>1)
    {
      Node node1 = pq.remove();
      Node node2 = pq.remove();
      //Source: http://stackoverflow.com/questions/12091506/what-is-the-backslash-character
      Node node = new Node(new Byte((byte)-1), node1.freq + node2.freq, node1, node2);
      pq.add(node);
    }
    return pq.remove();
  }
  public static Map<Byte, String> getCharacterCodes(Set<Byte> chars, Node node)
  {
    Map<Byte, String> map = new HashMap<Byte, String>();
    recursiveGetCode(node, map, "");
    return map;
  }
  public static void recursiveGetCode(Node node, Map<Byte, String> map, String s)
  {
    if(node.left == null && node.right == null)//leaf node
    {
      map.put(node.b, s);
      return;//base case
    }
    //First Traverse through the left, then the right branch
    recursiveGetCode(node.left, map, s+'0');
    recursiveGetCode(node.right, map, s+'1');
  }
  public static String getEncodedMessage(Map<Byte, String> map, byte[] s)
  {
    StringBuilder sb = new StringBuilder();
    for(Byte b:s)
    {
      sb.append(map.get(b));
    }
    return sb.toString();
  }
  public static void main(String[] args) throws Exception
  {
    String fileIn, fileOut, option;
    if(args.length != 4)
    {
      printUsage();
      System.exit(0);
    }
    else if(args[0].equals("--help") || args[0].equals("-h"))
    {
      printUsage();
      System.exit(0);
    }
    else
    {
      if(args[0].equals("--encode") || args[0].equals("-e"))
        encode = true;
      else if(args[0].equals("--decode") || args[0].equals("-d"))
        decode = true;
      else
      {
        printUsage();
        System.exit(0);
      }

      fileIn = args[1];
      fileOut = args[2];
      option = args[3];

      try{bi = new BitInputStream(new File(fileIn));}catch(Exception e){}
      try{bo = new BitOutputStream(new File(fileOut));}catch(IOException e){}
      //try{text = new Scanner( new File(fileIn) ).useDelimiter("\\A").next();}catch(Exception e){}
      try{pw = new PrintWriter(new File(fileOut));}catch(Exception e){}

      if(encode)
      {
        text = bi.allBytes();
        compress(text);
        bi.close();
      }
      if(decode)
      {
        expand();
        bi.close();
      }

      if(encode)
      {
        printEncode();
      }

      if(option.equals("--show-frequency")&&encode)
        showFrequency();
      else if(option.equals("--show-codes"))
        showCodes();
      else if(option.equals("--show-binary")&&encode)
        showBinary();
      else
      {
        printUsage();
        System.exit(0);
      }
      if(encode)
      {System.out.println(" input: "+bi.allBytes().length+ " bytes ["+bi.allBytes().length*8+" bits]");
      System.out.println("output: "+((header+encodedMessage.length())/8 + 1)+ " bytes [header: "+header+"; encoding: "+encodedMessage.length()+" bits]");
      System.out.println("output/input size: "+((float)((header+encodedMessage.length())/8 + 1)/bi.allBytes().length)*100+"%");}
      if(decode)
      {
        System.out.println("original size: "+length);
      }
    }
  }
  public static void printEncode() throws IOException
  {
    bo.writeInt(text.length);
    recursivePrint(root);
    header = bo.tally();
    for(char c: encodedMessage.toCharArray())
    {
      bo.writeBit(Character.getNumericValue(c));
    }
    bo.close();
  }
  public static void recursivePrint(Node n) throws IOException, NullPointerException
  {
    if(n.left == null && n.right == null)
    {
      bo.writeBit(1);
      bo.writeByte(n.b);
      return;
    }
    bo.writeBit(0);
    recursivePrint(n.left);
    recursivePrint(n.right);
  }
  public static void showFrequency()
  {
    System.out.println("FREQUENCY TABLE");
    IntComparator i = new IntComparator(FreqChar);
    TreeMap<Byte, Integer> sorted = new TreeMap<Byte, Integer>(i); // http://stackoverflow.com/questions/109383/how-to-sort-a-mapkey-value-on-the-values-in-java
    sorted.putAll(FreqChar);
    for(Map.Entry<Byte, Integer> entry : sorted.entrySet())
    {
      System.out.println("\'"+asASCIILiteral(entry.getKey())+"\': "+entry.getValue());
    }
  }
  public static void showCodes()
  {

      System.out.println("CODES");
      StringComparator s = new StringComparator(codes);
      TreeMap<Byte, String> sorted = new TreeMap<Byte, String>(s);
      sorted.putAll(codes);
      for(Map.Entry<Byte, String> entry : sorted.entrySet())
      {
        System.out.println("\""+entry.getValue()+"\" -> "+"\'"+asASCIILiteral(entry.getKey())+"\'");
      }
  }
  public static void showBinary()
  {
    System.out.println("ENCODED SEQUENCE");
    if(encode)
      System.out.println(encodedMessage);
    else
      {
        for(int i = 0; i < encodedSequence.length; i++)
        {
          System.out.print(encodedSequence[i]);
        }
        System.out.println();
      }
  }
  public static void printUsage()
  {
    System.out.println("Usage: java HuffmanCodes OPTIONS IN OUT");
    System.out.println("Encodes and decodes files using Huffman's technique");
    System.out.println("  -e, --encode               encodes IN to OUT");
    System.out.println("  -d, --decode               decodes IN to OUT");
    System.out.println("      --show-frequency       show the frequencies of each byte");
    System.out.println("      --show-codes           show the codes for each byte");
    System.out.println("      --show-binary          show the encoded sequence in binary");
    System.out.println("  -h, --help                 display this help and exit");
  }
}

class StringComparator implements Comparator<Byte>
{

    Map<Byte, String> base;
    public StringComparator(Map<Byte, String> base)
    {
        this.base = base;
    }
    //Source: http://stackoverflow.com/questions/5585779/converting-string-to-int-in-java
    public int compare(Byte a, Byte b)
    {
        if (Integer.valueOf(base.get(a)) > Integer.valueOf(base.get(b)))
        {
            return 1;
        } else
        {
            return -1;
        }
    }
}
class IntComparator implements Comparator<Byte>
{

    Map<Byte, Integer> base;
    public IntComparator(Map<Byte, Integer> base)
    {
        this.base = base;
    }

    public int compare(Byte a, Byte b)
    {
        if(base.get(a) == base.get(b))
        {
          if(a > b)
          {
            return 1;
          }
          else
          {
            return -1;
          }
        }
        else if (base.get(a) > base.get(b))
        {
            return 1;
        } else
        {
            return -1;
        }
    }
}
//Error 404: File Not Found
