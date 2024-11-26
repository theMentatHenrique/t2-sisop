import java.io.IOException;
import java.io.RandomAccessFile;

public class DirectoryManager {

    public DirEntry readDirEntry(int blockNumber, int entryIndex) {
        DirEntry entry = new DirEntry();
        try (RandomAccessFile fileStore = new RandomAccessFile("filesystem.dat", "r")) {
            int position = blockNumber * FileSystemParam.BLOCK_SIZE + entryIndex * FileSystemParam.DIR_ENTRY_SIZE;
            fileStore.seek(position);
    
            fileStore.readFully(entry.filename);
            entry.attributes = fileStore.readByte();
            entry.first_block = fileStore.readInt(); // Alterado para readInt
            entry.size = fileStore.readInt();
            return entry;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    

    public void writeDirEntry(int blockNumber, int entryIndex, DirEntry entry) {
        try (RandomAccessFile fileStore = new RandomAccessFile("filesystem.dat", "rw")) {
            int position = blockNumber * FileSystemParam.BLOCK_SIZE + entryIndex * FileSystemParam.DIR_ENTRY_SIZE;
            fileStore.seek(position);

            fileStore.write(entry.filename);
            fileStore.writeByte(entry.attributes);
            fileStore.writeInt(entry.first_block); // Alterado para writeInt
            fileStore.writeInt(entry.size);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
