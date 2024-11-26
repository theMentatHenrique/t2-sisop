import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Gerencia a leitura e escrita de entradas de diretório no sistema de arquivos.
 * Esta classe usa um arquivo binário chamado "filesystem.dat" para armazenar as informações do diretório.
 */
public class DirectoryManager {

    /**
     * Lê uma entrada de diretório de um bloco específico e posição no arquivo do sistema.
     * 
     * @param blockNumber o número do bloco onde o diretório está localizado.
     * @param entryIndex o índice da entrada dentro do bloco.
     * @return um objeto {@link DirEntry} com os dados lidos, ou {@code null} em caso de erro.
     */
    public DirEntry readDirEntry(int blockNumber, int entryIndex) {
        DirEntry entry = new DirEntry(); // Cria um objeto para armazenar os dados da entrada.
        try (RandomAccessFile fileStore = new RandomAccessFile("filesystem.dat", "r")) {
            // Calcula a posição no arquivo baseada no bloco e no índice da entrada.
            int position = blockNumber * FileSystemParam.BLOCK_SIZE + entryIndex * FileSystemParam.DIR_ENTRY_SIZE;
            fileStore.seek(position); // Move o cursor para a posição calculada.

            fileStore.readFully(entry.filename); // Nome do arquivo/diretório.
            entry.attributes = fileStore.readByte(); // Atributos (arquivo ou diretório).
            entry.first_block = fileStore.readInt(); // Primeiro bloco alocado.
            entry.size = fileStore.readInt(); // Tamanho do arquivo/diretório.
            return entry; // Retorna a entrada lida.
        } catch (IOException e) {
            e.printStackTrace(); 
            return null; 
        }
    }

    /**
     * Escreve uma entrada de diretório em um bloco específico e posição no arquivo do sistema.
     * 
     * @param blockNumber o número do bloco onde o diretório está localizado.
     * @param entryIndex o índice da entrada dentro do bloco.
     * @param entry um objeto {@link DirEntry} contendo os dados a serem gravados.
     */
    public void writeDirEntry(int blockNumber, int entryIndex, DirEntry entry) {
        try (RandomAccessFile fileStore = new RandomAccessFile("filesystem.dat", "rw")) {
            // Calcula a posição no arquivo baseada no bloco e no índice da entrada.
            int position = blockNumber * FileSystemParam.BLOCK_SIZE + entryIndex * FileSystemParam.DIR_ENTRY_SIZE;
            fileStore.seek(position); // Move o cursor para a posição calculada.

            fileStore.write(entry.filename); // Nome do arquivo/diretório.
            fileStore.writeByte(entry.attributes); // Atributos (arquivo ou diretório).
            fileStore.writeInt(entry.first_block); // Primeiro bloco alocado.
            fileStore.writeInt(entry.size); // Tamanho do arquivo/diretório.
        } catch (IOException e) {
            e.printStackTrace(); // Exibe o erro, caso ocorra.
        }
    }
}
