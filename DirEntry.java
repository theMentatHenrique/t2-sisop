public class DirEntry {
    public byte[] filename = new byte[25];
    public byte attributes;
    public int first_block; // Alterado de short para int
    public int size;
}
