import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

/**
 * Gerencia operações de leitura, escrita e inicialização de blocos no sistema de arquivos.
 */
public class BlockManager {

    /**
     * Lê um bloco do arquivo especificado.
     * 
     * @param file o nome do arquivo onde o bloco está armazenado.
     * @param block o número do bloco a ser lido.
     * @return os dados do bloco como um array de bytes.
     */
    public byte[] readBlock(String file, int block) {
        byte[] record = new byte[FileSystemParam.BLOCK_SIZE];
        try (RandomAccessFile fileStore = new RandomAccessFile(file, "rw")) {
            fileStore.seek(block * FileSystemParam.BLOCK_SIZE); // Move para o bloco.
            fileStore.readFully(record, 0, FileSystemParam.BLOCK_SIZE); // Lê o bloco.
        } catch (IOException e) {
            e.printStackTrace(); // Exibe erro.
        }
        return record;
    }

    /**
     * Escreve dados em um bloco do arquivo especificado.
     * 
     * @param file o nome do arquivo onde o bloco será escrito.
     * @param block o número do bloco a ser escrito.
     * @param data os dados a serem gravados.
     */
    public void writeBlock(String file, int block, byte[] data) {
        try (RandomAccessFile fileStore = new RandomAccessFile(file, "rw")) {
            fileStore.seek(block * FileSystemParam.BLOCK_SIZE); // Move para o bloco.
            fileStore.write(data, 0, FileSystemParam.BLOCK_SIZE); // Escreve os dados.
        } catch (IOException e) {
            e.printStackTrace(); 
        }
    }

    /**
     * Inicializa um bloco com valores vazios (zeros).
     * 
     * @param block o número do bloco a ser inicializado.
     */
    public void initializeBlock(int block) { 
        byte[] emptyBlock = new byte[FileSystemParam.BLOCK_SIZE]; // Bloco vazio.
        writeBlock("filesystem.dat", block, emptyBlock); // Escreve o bloco vazio.
    } 

    /**
     * Inicializa todos os blocos do sistema de arquivos com zeros.
     */
    public void initializeAllBlocks() { 
        byte[] allBlocks = new byte[FileSystemParam.BLOCK_SIZE * FileSystemParam.BLOCKS]; 
        Arrays.fill(allBlocks, (byte) 0); // Preenche todos os blocos com zeros.
        try (RandomAccessFile fileStore = new RandomAccessFile("filesystem.dat", "rw")) { 
            fileStore.seek(FileSystemParam.ROOT_BLOCK * FileSystemParam.BLOCK_SIZE + 1); // Move para a posição inicial.
            fileStore.write(allBlocks); // Escreve todos os blocos inicializados.
        } catch (IOException e) { 
            e.printStackTrace(); 
        } 
    }
}
