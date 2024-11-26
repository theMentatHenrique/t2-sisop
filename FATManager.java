import java.io.IOException;
import java.io.RandomAccessFile;

public class FATManager {
    private int[] fat = new int[FileSystemParam.BLOCKS];

    public void loadFAT(String file) {
        try (RandomAccessFile fileStore = new RandomAccessFile(file, "rw")) {
            fileStore.seek(0);
            for (int i = 0; i < FileSystemParam.BLOCKS; i++) {
                fat[i] = fileStore.readUnsignedShort();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveFAT(String file) {
        try (RandomAccessFile fileStore = new RandomAccessFile(file, "rw")) {
            fileStore.seek(0);
            for (int i = 0; i < FileSystemParam.BLOCKS; i++) {
                fileStore.writeShort(fat[i]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int allocateBlock() {
        for (int i = FileSystemParam.ROOT_BLOCK + 1; i < fat.length; i++) {
            if (fat[i] == 0x0000) { // Bloco livre
                fat[i] = 0x7fff; // Marca como fim de arquivo
                return i;
            }
        }
        return -1; // Sem blocos livres
    }

    public void freeBlock(int block) {
        fat[block] = 0x0000; // Marca como livre
    }

    public void freeChain(int startBlock) {
        int currentBlock = startBlock;
        while (currentBlock != 0x7fff && currentBlock != 0x0000) {
            int nextBlock = fat[currentBlock];
            fat[currentBlock] = 0x0000;
            if (nextBlock == 0x7fff || nextBlock == 0x0000) {
                break;
            }
            currentBlock = nextBlock;
        }
    }
    

    public int getFatValue(int index) {
        return fat[index];
    }

    public void setFatValue(int index, int value) {
        fat[index] = value;
    }

    public int[] getFat() {
        return fat;
    }
}
