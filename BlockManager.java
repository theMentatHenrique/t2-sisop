import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

public class BlockManager {
    public byte[] readBlock(String file, int block) {
        byte[] record = new byte[FileSystemParam.BLOCK_SIZE];
        try (RandomAccessFile fileStore = new RandomAccessFile(file, "rw")) {
            fileStore.seek(block * FileSystemParam.BLOCK_SIZE);
            fileStore.readFully(record, 0, FileSystemParam.BLOCK_SIZE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return record;
    }

    public void writeBlock(String file, int block, byte[] data) {
        try (RandomAccessFile fileStore = new RandomAccessFile(file, "rw")) {
            fileStore.seek(block * FileSystemParam.BLOCK_SIZE);
            fileStore.write(data, 0, FileSystemParam.BLOCK_SIZE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void initializeBlock(int block) { 
        byte[] emptyBlock = new byte[FileSystemParam.BLOCK_SIZE]; 
        writeBlock("filesystem.dat", block, emptyBlock); 
    } 
    
    public void initializeAllBlocks() { 
            byte[] allBlocks = new byte[FileSystemParam.BLOCK_SIZE * FileSystemParam.BLOCKS]; 
            Arrays.fill(allBlocks, (byte) 0); // Preenche todos os blocos com zeros 
            try (RandomAccessFile fileStore = new RandomAccessFile("filesystem.dat", "rw")) { 
                fileStore.seek(FileSystemParam.ROOT_BLOCK * FileSystemParam.BLOCK_SIZE + 1); 
                fileStore.write(allBlocks); 
            } 
            catch (IOException e){ 
                e.printStackTrace(); 
            } 
        }
}
