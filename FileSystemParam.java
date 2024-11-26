public class FileSystemParam {
	public final static int BLOCK_SIZE = 1024;
	public final static int BLOCKS = 2048;
	public final static int FAT_SIZE = BLOCKS * 2;
	public final static int FAT_BLOCKS = FAT_SIZE / BLOCK_SIZE;
	public final static int ROOT_BLOCK = FAT_BLOCKS;
	public final static int DIR_ENTRY_SIZE = 32;
	public final static int DIR_ENTRIES = BLOCK_SIZE / DIR_ENTRY_SIZE;
}
