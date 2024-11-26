import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileSystemShell {
    private FATManager fatManager = new FATManager();
    private DirectoryManager dirManager = new DirectoryManager();
    private BlockManager blockManager = new BlockManager();
    private int currentDirectoryBlock = FileSystemParam.ROOT_BLOCK; // Diretório atual

    public void runShell() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("FileSystem Shell. Type 'help' for commands.");
        while (true) {
            System.out.print("\n> ");
            String command = scanner.nextLine().trim();
            if (command.equalsIgnoreCase("exit")) {
                break;
            }
            executeCommand(command);
        }
        scanner.close();
    }

    public void executeCommand(String commandLine) {
        String[] parts = commandLine.split(" ", 2);
        String cmd = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : null;

        try {
            switch (cmd) {
                case "init":
                    initializeFileSystem();
                    break;
                case "load":
                    loadFileSystem();
                    break;
                case "ls":
                    listDirectory(args);
                    break;
                case "mkdir":
                    createDirectory(args);
                    break;
                case "create":
                    createFile(args);
                    break;
                case "unlink":
                    deleteFileOrDirectory(args);
                    break;
                case "cd":
                    changeDirectory(args);
                    break;
                case "tree":
                    displayTree(currentDirectoryBlock, 0);
                    break;
                case "write":
                    writeToFile(args);
                    break;
                case "append":
                    appendToFile(args);
                    break;
                case "read":
                    readFromFile(args);
                    break;
                case "check":
                    checkConsistency();
                    break;
                case "stats":
                    showStats();
                    break;
                case "help":
                    showHelp();
                    break;
                default:
                    System.out.println("Unknown command. Type 'help' for a list of commands.");
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void initializeFileSystem() {
        // Inicializa a FAT
        int[] fat = fatManager.getFat();
        for (int i = 0; i < FileSystemParam.FAT_BLOCKS; i++) {
            fat[i] = 0x7ffe; // Reservado para a FAT
        }
        fat[FileSystemParam.ROOT_BLOCK] = 0x7fff; // Diretório raiz
        for (int i = FileSystemParam.ROOT_BLOCK + 1; i < FileSystemParam.BLOCKS; i++) {
            fat[i] = 0x0000; // Blocos livres
        }
        fatManager.saveFAT("filesystem.dat");

        // Inicializa o bloco do diretório raiz
        blockManager.initializeBlock(FileSystemParam.ROOT_BLOCK);

        // Inicializa todos os blocos de dados
        blockManager.initializeAllBlocks(); 

        currentDirectoryBlock = FileSystemParam.ROOT_BLOCK;
        System.out.println("FileSystem initialized.");
    }

    private void listDirectory(String path) {
        int directoryToList = currentDirectoryBlock;
        if (path != null && !path.isEmpty()) {
            directoryToList = navigateToPath(path);
            if (directoryToList == -1) {
                System.out.println("Error: Directory not found.");
                return;
            }
        }
        System.out.println("Listing directory:");
        for (int i = 0; i < FileSystemParam.DIR_ENTRIES; i++) {
            DirEntry entry = dirManager.readDirEntry(directoryToList, i);
            if (entry != null && entry.attributes != 0) {
                String type = (entry.attributes == 0x01) ? "File" : "Directory";
                System.out.println(type + ": " + new String(entry.filename).trim());
            }
        }
    }

    private int navigateToPath(String path) {
        if (path == null || path.isEmpty()) {
            return currentDirectoryBlock;
        }
        int directoryToSearch = currentDirectoryBlock;
        if (path.startsWith("/")) {
            directoryToSearch = FileSystemParam.ROOT_BLOCK;
            path = path.substring(1);
        }
        String[] parts = path.split("/");
        for (String part : parts) {
            if (part.isEmpty() || part.equals(".")) {
                continue;
            }
            if (part.equals("..")) {
                // Implementar se necessário

                continue;
            }
            boolean found = false;
            for (int i = 0; i < FileSystemParam.DIR_ENTRIES; i++) {
                DirEntry entry = dirManager.readDirEntry(directoryToSearch, i);
                if (entry != null && entry.attributes != 0 && new String(entry.filename).trim().equals(part)) {
                    if (entry.attributes == 0x02) { // Diretório
                        directoryToSearch = entry.first_block;
                        found = true;
                        break;
                    } else {
                        return entry.first_block; // Arquivo encontrado
                    }
                }
            }
            if (!found) {
                return -1;
            }
        }
        return directoryToSearch;
    }

    private int navigateToParentDirectory(String path) {
        if (path == null || path.isEmpty()) {
            return currentDirectoryBlock;
        }
        int directoryToSearch = currentDirectoryBlock;
        if (path.startsWith("/")) {
            directoryToSearch = FileSystemParam.ROOT_BLOCK;
            path = path.substring(1);
        }
        String[] parts = path.split("/");
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            if (part.isEmpty() || part.equals(".")) {
                continue;
            }
            boolean found = false;
            for (int j = 0; j < FileSystemParam.DIR_ENTRIES; j++) {
                DirEntry entry = dirManager.readDirEntry(directoryToSearch, j);
                if (entry != null && entry.attributes == 0x02 && new String(entry.filename).trim().equals(part)) {
                    directoryToSearch = entry.first_block;
                    found = true;
                    break;
                }
            }
            if (!found) {
                return -1;
            }
        }
        return directoryToSearch;
    }

    private void createDirectory(String path) {
        if (path == null || path.isEmpty()) {
            System.out.println("Usage: mkdir /path");
            return;
        }

        int parentBlock = currentDirectoryBlock; // Por padrão, começa no diretório atual

        // Se o caminho for absoluto, comece no root
        if (path.startsWith("/")) {
            parentBlock = FileSystemParam.ROOT_BLOCK;
            path = path.substring(1); // Remove a barra inicial
        }

        // Divide o caminho em partes
        String[] parts = path.split("/");
        String dirName = parts[parts.length - 1]; // Nome do diretório a ser criado

        // Navega até o diretório pai
        for (int i = 0; i < parts.length - 1; i++) {
            boolean found = false;
            for (int j = 0; j < FileSystemParam.DIR_ENTRIES; j++) {
                DirEntry entry = dirManager.readDirEntry(parentBlock, j);
                if (entry.attributes == 0x02 && new String(entry.filename).trim().equals(parts[i])) {
                    parentBlock = entry.first_block;
                    found = true;
                    break;
                }
            }
            if (!found) {
                System.out.println("Error: Directory not found in path.");
                return;
            }
        }

        // Verifica se o diretório já existe no diretório pai
        for (int i = 0; i < FileSystemParam.DIR_ENTRIES; i++) {
            DirEntry entry = dirManager.readDirEntry(parentBlock, i);
            if (entry.attributes != 0 && new String(entry.filename).trim().equals(dirName)) {
                System.out.println("Error: Directory already exists.");
                return;
            }
        }

        // Cria o novo diretório
        DirEntry newDir = new DirEntry();
        byte[] nameBytes = dirName.getBytes();
        for (int i = 0; i < nameBytes.length; i++) {
            newDir.filename[i] = nameBytes[i];
        }
        newDir.attributes = 0x02; // Diretório
        newDir.first_block = (short) fatManager.allocateBlock();
        newDir.size = 0;

        // Salva o diretório no diretório pai
        for (int i = 0; i < FileSystemParam.DIR_ENTRIES; i++) {
            DirEntry entry = dirManager.readDirEntry(parentBlock, i);
            if (entry.attributes == 0x00) { // Entrada vazia
                dirManager.writeDirEntry(parentBlock, i, newDir);
                blockManager.initializeBlock(newDir.first_block); // Inicializa o bloco do novo diretório
                System.out.println("Directory created: " + path);
                return;
            }
        }

        System.out.println("Error: No space available in the current directory.");
    }

    private void createFile(String path) {
        if (path == null || path.isEmpty()) {
            System.out.println("Usage: create /path/file");
            return;
        }
        int parentBlock = navigateToParentDirectory(path);
        if (parentBlock == -1) {
            System.out.println("Error: Directory not found.");
            return;
        }
        String fileName = extractFileName(path);
        if (fileName == null || fileName.isEmpty()) {
            System.out.println("Error: Invalid file name.");
            return;
        }
        // Verifica se o arquivo já existe
        for (int i = 0; i < FileSystemParam.DIR_ENTRIES; i++) {
            DirEntry entry = dirManager.readDirEntry(parentBlock, i);
            if (entry != null && entry.attributes != 0 && new String(entry.filename).trim().equals(fileName)) {
                System.out.println("Error: File already exists.");
                return;
            }
        }
        // Cria o novo arquivo
        DirEntry newFile = new DirEntry();
        byte[] nameBytes = fileName.getBytes();
        System.arraycopy(nameBytes, 0, newFile.filename, 0, Math.min(nameBytes.length, newFile.filename.length));
        newFile.attributes = 0x01; // Arquivo regular
        int firstBlock = fatManager.allocateBlock();
        if (firstBlock == -1) {
            System.out.println("Error: No space left on device.");
            return;
        }
        newFile.first_block = (short) firstBlock;
        newFile.size = 0;
        // Salva a nova entrada de arquivo no diretório pai
        for (int i = 0; i < FileSystemParam.DIR_ENTRIES; i++) {
            DirEntry entry = dirManager.readDirEntry(parentBlock, i);
            if (entry == null || entry.attributes == 0x00) {
                dirManager.writeDirEntry(parentBlock, i, newFile);
                fatManager.saveFAT("filesystem.dat");
                System.out.println("File created: " + path);
                return;
            }
        }
        System.out.println("Error: No space available in the directory.");
    }

    private void deleteFileOrDirectory(String path) {
        if (path == null || path.isEmpty()) {
            System.out.println("Usage: unlink /path/file_or_directory");
            return;
        }

        int parentBlock = navigateToParentDirectory(path);
        if (parentBlock == -1) {
            System.out.println("Error: Directory not found.");
            return;
        }

        String name = extractFileName(path);
        if (name == null || name.isEmpty()) {
            System.out.println("Error: Invalid name.");
            return;
        }

        for (int i = 0; i < FileSystemParam.DIR_ENTRIES; i++) {
            DirEntry entry = dirManager.readDirEntry(parentBlock, i);
            if (entry != null && new String(entry.filename).trim().equals(name) && entry.attributes != 0x00) {
                if (entry.attributes == 0x02) { // Diretório
                    if (!isDirectoryEmpty(entry.first_block)) {
                        System.out.println("Error: Directory is not empty.");
                        return;
                    }
                }
                // Free all blocks associated
                fatManager.freeChain(entry.first_block);
                entry.attributes = 0x00; // Mark entry as empty
                dirManager.writeDirEntry(parentBlock, i, entry);
                fatManager.saveFAT("filesystem.dat");
                System.out.println("Deleted: " + path);
                return;
            }
        }
        System.out.println("Error: File or directory not found.");
    }

    private boolean isDirectoryEmpty(int block) {
        for (int i = 0; i < FileSystemParam.DIR_ENTRIES; i++) {
            DirEntry entry = dirManager.readDirEntry(block, i);
            if (entry.attributes != 0) {
                return false;
            }
        }
        return true;
    }

    private void loadFileSystem() {
        try {
            fatManager.loadFAT("filesystem.dat");
            currentDirectoryBlock = FileSystemParam.ROOT_BLOCK;
            System.out.println("FileSystem loaded from disk.");
        } catch (Exception e) {
            System.out.println("Error: Could not load the file system. " + e.getMessage());
        }
    }

    private void changeDirectory(String path) {
        if (path == null || path.isEmpty()) {
            System.out.println("Usage: cd /path");
            return;
        }

        int directoryToSearch = currentDirectoryBlock; // Começa no diretório atual

        // Se o caminho for absoluto, reinicie no root
        if (path.startsWith("/")) {
            directoryToSearch = FileSystemParam.ROOT_BLOCK;
            path = path.substring(1); // Remove a barra inicial
        }

        if (path.isEmpty()) {
            // O caminho é apenas "/", então definimos o diretório atual como raiz
            currentDirectoryBlock = directoryToSearch;
            System.out.println("Changed to root directory");
            return;
        }

        // Divide o caminho em partes
        String[] parts = path.split("/");
        for (String part : parts) {
            if (part.isEmpty() || part.equals(".")) {
                continue;
            }
            boolean found = false;
            for (int i = 0; i < FileSystemParam.DIR_ENTRIES; i++) {
                DirEntry entry = dirManager.readDirEntry(directoryToSearch, i);
                if (entry != null && entry.attributes == 0x02 && new String(entry.filename).trim().equals(part)) {
                    directoryToSearch = entry.first_block; // Avança para o próximo bloco
                    found = true;
                    break;
                }
            }
            if (!found) {
                System.out.println("Error: Directory not found.");
                return;
            }
        }

        currentDirectoryBlock = directoryToSearch; // Atualiza o diretório atual
        System.out.println("Changed to directory: " + path);
    }

    private String extractFileName(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        String[] parts = path.split("/");
        return parts[parts.length - 1];
    }

    private void writeToFile(String args) {
        if (args == null || args.isEmpty()) {
            System.out.println("Usage: write \"data\" [rep] /path/file");
            return;
        }

        // Regex pattern to match the command arguments
        Pattern pattern = Pattern.compile("^\"([^\"]*)\"(?:\\s+(\\d+))?\\s+(.+)$");
        Matcher matcher = pattern.matcher(args);

        if (!matcher.matches()) {
            System.out.println("Usage: write \"data\" [rep] /path/file");
            return;
        }

        String data = matcher.group(1);
        String repStr = matcher.group(2);
        String path = matcher.group(3);

        int rep = 1;
        if (repStr != null) {
            try {
                rep = Integer.parseInt(repStr);
            } catch (NumberFormatException e) {
                System.out.println("Error: Invalid repetition count.");
                return;
            }
        }

        // Repeat data as per the repetition count
        StringBuilder dataBuilder = new StringBuilder();
        for (int i = 0; i < rep; i++) {
            dataBuilder.append(data);
        }
        String dataToWrite = dataBuilder.toString();

        // The rest of your method remains the same...
        // Navigate to the parent directory of the file
        int parentDirectoryBlock = navigateToParentDirectory(path);
        if (parentDirectoryBlock == -1) {
            System.out.println("Error: Directory not found.");
            return;
        }

        String fileName = extractFileName(path);
        if (fileName == null || fileName.isEmpty()) {
            System.out.println("Error: Invalid file name.");
            return;
        }

        // Find the file entry in the parent directory
        DirEntry entry = null;
        int entryIndex = -1;
        for (int i = 0; i < FileSystemParam.DIR_ENTRIES; i++) {
            DirEntry dirEntry = dirManager.readDirEntry(parentDirectoryBlock, i);
            if (dirEntry != null && new String(dirEntry.filename).trim().equals(fileName)
                    && dirEntry.attributes == 0x01) {
                entry = dirEntry;
                entryIndex = i;
                break;
            }
        }

        if (entry == null) {
            System.out.println("Error: File not found.");
            return;
        }

        // Free the existing blocks of the file in the FAT
        fatManager.freeChain(entry.first_block);

        // Start writing the new content
        byte[] dataBytes = dataToWrite.getBytes();
        int remainingData = dataBytes.length;
        int offset = 0;

        // Alocar o primeiro bloco
        int currentBlock = fatManager.allocateBlock();
        if (currentBlock == -1) {
            System.out.println("Error: No space left on device.");
            return;
        }
        entry.first_block = currentBlock;

        // Write data into blocks
        while (remainingData > 0) {
            byte[] blockData = new byte[FileSystemParam.BLOCK_SIZE];
            int bytesToWrite = Math.min(FileSystemParam.BLOCK_SIZE, remainingData);
            System.arraycopy(dataBytes, offset, blockData, 0, bytesToWrite);

            blockManager.writeBlock("filesystem.dat", currentBlock, blockData);
            remainingData -= bytesToWrite;
            offset += bytesToWrite;

            if (remainingData > 0) {
                int nextBlock = fatManager.allocateBlock();
                if (nextBlock == -1) {
                    System.out.println("Error: No space left on device.");
                    return;
                }
                fatManager.setFatValue(currentBlock, (short) nextBlock);
                currentBlock = nextBlock;
            } else {
                fatManager.setFatValue(currentBlock, (short) 0x7fff);
            }
        }

        // Update the file size and save the directory entry
        entry.size = dataBytes.length;
        dirManager.writeDirEntry(parentDirectoryBlock, entryIndex, entry);

        // Save the FAT
        fatManager.saveFAT("filesystem.dat");

        System.out.println("Data written to file: " + path);
    }

    private void appendToFile(String args) {
        if (args == null || args.isEmpty()) {
            System.out.println("Usage: append \"data\" [rep] /path/file");
            return;
        }

        // Atualização do padrão regex conforme a resposta anterior
        Pattern pattern = Pattern.compile("^\\s*\"(.*?)\"(?:\\s+(\\d+))?\\s+(.+)$");
        Matcher matcher = pattern.matcher(args);

        if (!matcher.matches()) {
            System.out.println("Usage: append \"data\" [rep] /path/file");
            return;
        }

        String data = matcher.group(1);
        String repStr = matcher.group(2);
        String path = matcher.group(3);

        int rep = 1;
        if (repStr != null) {
            try {
                rep = Integer.parseInt(repStr);
            } catch (NumberFormatException e) {
                System.out.println("Error: Invalid repetition count.");
                return;
            }
        }

        // Repete os dados conforme o valor de repetições
        StringBuilder dataBuilder = new StringBuilder();
        for (int i = 0; i < rep; i++) {
            dataBuilder.append(data);
        }
        String dataToAppend = dataBuilder.toString();

        // Navegar até o diretório pai do arquivo
        int parentDirectoryBlock = navigateToParentDirectory(path);
        if (parentDirectoryBlock == -1) {
            System.out.println("Error: Directory not found.");
            return;
        }

        String fileName = extractFileName(path);
        if (fileName == null || fileName.isEmpty()) {
            System.out.println("Error: Invalid file name.");
            return;
        }

        // Encontrar a entrada do arquivo no diretório pai
        DirEntry entry = null;
        int entryIndex = -1;
        for (int i = 0; i < FileSystemParam.DIR_ENTRIES; i++) {
            DirEntry dirEntry = dirManager.readDirEntry(parentDirectoryBlock, i);
            if (dirEntry != null && new String(dirEntry.filename).trim().equals(fileName)
                    && dirEntry.attributes == 0x01) {
                entry = dirEntry;
                entryIndex = i;
                break;
            }
        }

        if (entry == null) {
            System.out.println("Error: File not found.");
            return;
        }

        byte[] dataBytes = dataToAppend.getBytes();
        int remainingData = dataBytes.length;
        int offset = 0;
        int lastBlock = entry.first_block;
        int fileSize = entry.size;


        // Navega até o último bloco
        Set<Integer> visitedBlocks = new HashSet<>();
        while (fatManager.getFatValue(lastBlock) != 0x7fff) {
            if (visitedBlocks.contains(lastBlock)) {
                System.out.println("Error: Loop detected in FAT chain at block " + lastBlock);
                return;
            }
            visitedBlocks.add(lastBlock);

            int nextBlock = fatManager.getFatValue(lastBlock);
            if (nextBlock <= 0 || nextBlock >= FileSystemParam.BLOCKS) {
                System.out.println("Error: Invalid FAT value at block " + lastBlock + ": " + nextBlock);
                return;
            }

            lastBlock = nextBlock;
        }

        // Calcula o espaço livre no último bloco
        int freeSpaceInLastBlock = FileSystemParam.BLOCK_SIZE - (entry.size % FileSystemParam.BLOCK_SIZE);
        int bytesToWrite = Math.min(freeSpaceInLastBlock, remainingData);

        if (bytesToWrite > 0 && freeSpaceInLastBlock > 0) {
            byte[] lastBlockData = blockManager.readBlock("filesystem.dat", lastBlock);
            System.arraycopy(dataBytes, offset, lastBlockData, FileSystemParam.BLOCK_SIZE - freeSpaceInLastBlock,
                    bytesToWrite);
            blockManager.writeBlock("filesystem.dat", lastBlock, lastBlockData);
            remainingData -= bytesToWrite;
            offset += bytesToWrite;
            fileSize += bytesToWrite;
        }

        // Aloca novos blocos para os dados restantes
        int currentBlock = lastBlock;
        while (remainingData > 0) {
            int nextBlock = fatManager.allocateBlock();
            if (nextBlock == -1) {
                System.out.println("Error: No space left on device.");
                return;
            }
            fatManager.setFatValue(currentBlock, nextBlock);
            currentBlock = nextBlock;

            bytesToWrite = Math.min(FileSystemParam.BLOCK_SIZE, remainingData);
            byte[] blockData = new byte[FileSystemParam.BLOCK_SIZE];
            System.arraycopy(dataBytes, offset, blockData, 0, bytesToWrite);
            blockManager.writeBlock("filesystem.dat", currentBlock, blockData);

            remainingData -= bytesToWrite;
            offset += bytesToWrite;
            fileSize += bytesToWrite;
        }

        // Marca o fim do arquivo na FAT
        fatManager.setFatValue(currentBlock, 0x7fff);

        // Atualiza o tamanho do arquivo e salva a entrada de diretório
        entry.size = fileSize;
        dirManager.writeDirEntry(parentDirectoryBlock, entryIndex, entry);

        // Salva a FAT
        fatManager.saveFAT("filesystem.dat");

        System.out.println("Data appended to file: " + path);
    }

    private void readFromFile(String path) {
        if (path == null || path.isEmpty()) {
            System.out.println("Usage: read /path/file");
            return;
        }

        // Navigate to the parent directory of the file
        int parentDirectoryBlock = navigateToParentDirectory(path);
        if (parentDirectoryBlock == -1) {
            System.out.println("Error: Directory not found.");
            return;
        }

        String fileName = extractFileName(path);
        if (fileName == null || fileName.isEmpty()) {
            System.out.println("Error: Invalid file name.");
            return;
        }

        // Find the file entry in the parent directory
        DirEntry entry = null;
        for (int i = 0; i < FileSystemParam.DIR_ENTRIES; i++) {
            DirEntry dirEntry = dirManager.readDirEntry(parentDirectoryBlock, i);
            if (dirEntry != null && new String(dirEntry.filename).trim().equals(fileName)
                    && dirEntry.attributes == 0x01) {
                entry = dirEntry;
                break;
            }
        }

        if (entry == null) {
            System.out.println("Error: File not found.");
            return;
        }

        // Read all blocks of the file by following the FAT chain
        int currentBlock = entry.first_block;
        StringBuilder fileData = new StringBuilder();
        int bytesRead = 0;

        while (currentBlock != 0x7fff && currentBlock != 0x0000 && bytesRead < entry.size) {
            byte[] data = blockManager.readBlock("filesystem.dat", currentBlock);
            int bytesToRead = Math.min(FileSystemParam.BLOCK_SIZE, entry.size - bytesRead);
            fileData.append(new String(data, 0, bytesToRead));
            bytesRead += bytesToRead;
            currentBlock = fatManager.getFatValue(currentBlock);
        }

        System.out.println("Data in file: " + path + ": " + fileData.toString());
    }

    private void displayTree(int block, int depth) {
        for (int i = 0; i < FileSystemParam.DIR_ENTRIES; i++) {
            DirEntry entry = dirManager.readDirEntry(block, i);
            if (entry.attributes != 0) {
                for (int j = 0; j < depth; j++) {
                    System.out.print("  ");
                }
                System.out.println("- " + new String(entry.filename).trim() +
                        (entry.attributes == 0x02 ? "/" : ""));
                if (entry.attributes == 0x02) {
                    displayTree(entry.first_block, depth + 1);
                }
            }
        }
    }

    private void checkConsistency() {
        boolean[] blocksUsed = new boolean[FileSystemParam.BLOCKS];

        // Mark reserved blocks (FAT)
        for (int i = 0; i < FileSystemParam.FAT_BLOCKS; i++) {
            blocksUsed[i] = true;
        }

        // Check the file system starting from the root directory
        boolean hasInconsistencies = checkDirectoryConsistency(FileSystemParam.ROOT_BLOCK, blocksUsed);

        // Check for allocated blocks that are not referenced
        for (int i = 0; i < FileSystemParam.BLOCKS; i++) {
            if (fatManager.getFatValue(i) != 0x0000 && !blocksUsed[i]) {
                System.out.println("Inconsistency: Block " + i + " is allocated but not referenced.");
                hasInconsistencies = true;
            }
        }

        if (!hasInconsistencies) {
            System.out.println("FileSystem is consistent.");
        }
    }

    private boolean checkDirectoryConsistency(int dirBlock, boolean[] blocksUsed) {
        boolean hasInconsistencies = false;
        blocksUsed[dirBlock] = true;

        for (int i = 0; i < FileSystemParam.DIR_ENTRIES; i++) {
            DirEntry entry = dirManager.readDirEntry(dirBlock, i);
            if (entry != null && entry.attributes != 0x00) {
                int firstBlock = entry.first_block;
                if (firstBlock <= 0 || firstBlock >= FileSystemParam.BLOCKS) {
                    System.out.println(
                            "Inconsistency: Entry " + new String(entry.filename).trim() + " has invalid first block.");
                    hasInconsistencies = true;
                    continue;
                }

                if (blocksUsed[firstBlock]) {
                    System.out.println("Inconsistency: Block " + firstBlock + " is already used.");
                    hasInconsistencies = true;
                    continue;
                }

                // Mark blocks used by the file or directory
                if (entry.attributes == 0x01) { // File
                    hasInconsistencies |= checkFileConsistency(firstBlock, blocksUsed, entry.size, entry.filename);
                } else if (entry.attributes == 0x02) { // Directory
                    hasInconsistencies |= checkDirectoryConsistency(firstBlock, blocksUsed);
                }
            }
        }
        return hasInconsistencies;
    }

    private boolean checkFileConsistency(int firstBlock, boolean[] blocksUsed, int fileSize, byte[] filename) {
        boolean hasInconsistencies = false;
        int currentBlock = firstBlock;
        int totalSize = 0;

        while (currentBlock != 0x7fff && currentBlock != 0x0000) {
            if (blocksUsed[currentBlock]) {
                System.out.println("Inconsistency: Block " + currentBlock + " is already used.");
                hasInconsistencies = true;
                break;
            }
            blocksUsed[currentBlock] = true;
            totalSize += FileSystemParam.BLOCK_SIZE;
            currentBlock = fatManager.getFatValue(currentBlock);
        }

        if (currentBlock == 0x0000) {
            System.out.println("Inconsistency: File " + new String(filename).trim() + " chain terminated improperly.");
            hasInconsistencies = true;
        }

        // Check if the file size is consistent
        if (totalSize < fileSize) {
            System.out.println("Inconsistency: File " + new String(filename).trim() + " size mismatch.");
            hasInconsistencies = true;
        }

        return hasInconsistencies;
    }

    private void showStats() {
        int freeBlocks = 0;
        int usedBlocks = 0;

        for (int i = 0; i < FileSystemParam.BLOCKS; i++) {
            if (fatManager.getFatValue(i) == 0x0000) {
                freeBlocks++;
            } else {
                usedBlocks++;
            }
        }

        System.out.println("FileSystem Stats:");
        System.out.println("Total Blocks: " + FileSystemParam.BLOCKS);
        System.out.println("Used Blocks: " + usedBlocks);
        System.out.println("Free Blocks: " + freeBlocks);
        System.out.println("Block Size: " + FileSystemParam.BLOCK_SIZE + " bytes");
    }

    private void showHelp() {
        System.out.println("Available commands:");
        System.out.println("  init                              - Initialize the file system");
        System.out.println("  load                              - Load the file system from disk");
        System.out.println("  ls [/path]                        - List directory contents");
        System.out.println("  mkdir /path                       - Create a new directory");
        System.out.println("  create /path/file                 - Create a new file");
        System.out.println("  unlink /path/file                 - Delete a file or directory");
        System.out.println("  cd /path                          - Change directory");
        System.out.println("  write \"data\" [rep] /path/file   - Write data to a file");
        System.out.println("  append \"data\" [rep] /path/file  - Append data to a file");
        System.out.println("  read /path/file                   - Read data from a file");
        System.out.println("  tree                              - Display directory structure");
        System.out.println("  stats                             - Show file system statistics");
        System.out.println("  check                             - Check file system consistency");
        System.out.println("  help                              - Show this help message");
        System.out.println("  exit                              - Exit the shell");
    }
}