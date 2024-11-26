import java.io.IOException;
import java.io.RandomAccessFile;

public class FATManager {
    private int[] fat = new int[FileSystemParam.BLOCKS]; // Array para representar a FAT.

    /**
     * Carrega a FAT de um arquivo.
     * @param file o nome do arquivo onde a FAT está armazenada.
     */
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

    /**
     * Salva a FAT em um arquivo.
     * @param file o nome do arquivo onde a FAT será gravada.
     */
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

    /**
     * Aloca o próximo bloco livre.
     * @return o índice do bloco alocado, ou -1 se não houver blocos livres.
     */
    public int allocateBlock() {
        for (int i = FileSystemParam.ROOT_BLOCK + 1; i < fat.length; i++) {
            if (fat[i] == 0x0000) { // Verifica se o bloco está livre.
                fat[i] = 0x7fff; // Marca como fim de arquivo.
                return i;
            }
        }
        return -1; // Sem blocos livres.
    }

    /**
     * Libera um bloco, marcando-o como livre.
     * @param block o índice do bloco a ser liberado.
     */
    public void freeBlock(int block) {
        fat[block] = 0x0000; // Marca o bloco como livre.
    }

    /**
     * Libera todos os blocos em uma cadeia a partir de um bloco inicial.
     * @param startBlock o bloco inicial da cadeia.
     */
    public void freeChain(int startBlock) {
        int currentBlock = startBlock;
        while (currentBlock != 0x7fff && currentBlock != 0x0000) {
            int nextBlock = fat[currentBlock];
            fat[currentBlock] = 0x0000; // Marca o bloco atual como livre.
            if (nextBlock == 0x7fff || nextBlock == 0x0000) {
                break;
            }
            currentBlock = nextBlock; // Avança para o próximo bloco.
        }
    }

    /**
     * Retorna o valor da FAT em um índice específico.
     * @param index o índice da FAT.
     * @return o valor no índice.
     */
    public int getFatValue(int index) {
        return fat[index];
    }

    /**
     * Define um valor na FAT em um índice específico.
     * @param index o índice da FAT.
     * @param value o valor a ser definido.
     */
    public void setFatValue(int index, int value) {
        fat[index] = value;
    }

    /**
     * Retorna o array da FAT.
     * @return o array completo da FAT.
     */
    public int[] getFat() {
        return fat;
    }
}
