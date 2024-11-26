import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Classe responsável por solicitar e gerenciar os comandos dados pelo terminal
 * Comandos incluem : ler diretório, criar diretório, etc 
 */
public class FileSystemShell {
    private FATManager fatManager = new FATManager();
    private DirectoryManager dirManager = new DirectoryManager();
    private BlockManager blockManager = new BlockManager();
    private int currentDirectoryBlock = FileSystemParam.ROOT_BLOCK; // Diretório atual

    /**
     * Exibe terminal infinitamente e captura comando inserido
     */
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

    
    /**
     * Chama o método responsável por tratar o comando vindo do terminal
     *
     * @param commandLine o comando capturado do terminal
     */  
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

     /**
     * Inicializa a FAT (File Allocation Table)
     * Reserva os blocos da própria FAT
     * Reserva o diretório Raiz
     * Aloca todos os demais blocos após o dir raiz até o final
     */
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

    
    /**
     * Lista o conteúdo do diretório especificado ou do diretório atual caso nenhum caminho seja fornecido.
     * 
     * @param path o caminho do diretório a ser listado. Se for vazio, o diretório atual será listado.
     *             O caminho pode ser absoluto (iniciando com "/") ou relativo ao diretório atual.
     *     
     * @apiNote Este método imprime o conteúdo do diretório no console. Cada entrada será exibida como:
     *          "File: <nome_do_arquivo>" para arquivos ou "Directory: <nome_do_diretório>" para diretórios.
     */
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


    /**
     * Navega até o diretório ou arquivo especificado pelo caminho fornecido e retorna o bloco correspondente.
     * 
     * @param path o caminho do diretório ou arquivo a ser navegado. Pode ser absoluto (começando com "/") ou
     *             relativo ao diretório atual. 
     * @return o número do bloco do diretório ou arquivo encontrado. Retorna:
     *         - O bloco do diretório ou arquivo, se encontrado.
     *         - -1 caso o caminho não exista.
     *         - O bloco do diretório atual, se o caminho for vazio.
     */
    private int navigateToPath(String path) {
        // retorna caminho Atual se vazio
        if (path == null || path.isEmpty()) {
            return currentDirectoryBlock;
        }

        int directoryToSearch = currentDirectoryBlock;

        // Se o caminho começar com "/", define o ponto inicial como o diretório raiz.
        if (path.startsWith("/")) {
            directoryToSearch = FileSystemParam.ROOT_BLOCK;
            path = path.substring(1); // Remove a barra inicial para simplificar busca
        }

        // Divide o caminho em partes usando "/" como delimitador.
        String[] parts = path.split("/");

        // Itera sobre cada parte do caminho para navegar.
        for (String part : parts) {
            // Ignora partes vazias ou que representam o diretório atual (".").
            if (part.isEmpty() || part.equals(".")) {
                continue;
            }

            // Trata o caso de subir para o diretório pai ("..").
            if (part.equals("..")) {
                // Implementação para subir ao diretório pai pode ser adicionada aqui, se necessário.
                continue;
            }

            boolean found = false; // Flag para indicar se a parte atual foi encontrada.

            // Itera pelas entradas do diretório atual para localizar a parte atual.
            for (int i = 0; i < FileSystemParam.DIR_ENTRIES; i++) {
                DirEntry entry = dirManager.readDirEntry(directoryToSearch, i);

                // Verifica se a entrada é válida e corresponde à parte atual.
                if (entry != null && entry.attributes != 0 && new String(entry.filename).trim().equals(part)) {
                    if (entry.attributes == 0x02) { // Se for um diretório.
                        directoryToSearch = entry.first_block; // Atualiza para o próximo diretório.
                        found = true;
                        break;
                    } else {
                        return entry.first_block; // Retorna o bloco do arquivo encontrado.
                    }
                }
            }

            // Se não encontrou, retorna -1
            if (!found) {
                return -1;
            }
        }

        // Retorna o bloco do diretório encontrado após processar todas as partes do caminho.
        return directoryToSearch;
    }

    /**
     * Navega até o diretório pai do caminho especificado.
     * 
     * @param path o caminho do arquivo ou diretório. Pode ser absoluto ou relativo.
     * @return o bloco do diretório pai ou -1 se o caminho for inválido.
     */
    private int navigateToParentDirectory(String path) {
        // Retorna o diretório atual se o caminho for vazio.
        if (path == null || path.isEmpty()) {
            return currentDirectoryBlock;
        }

        // Define o ponto de partida como o diretório atual ou raiz, se o caminho for absoluto.
        int directoryToSearch = currentDirectoryBlock;
        if (path.startsWith("/")) {
            directoryToSearch = FileSystemParam.ROOT_BLOCK;
            path = path.substring(1);
        }

        // Divide o caminho em partes e ignora a última, já que queremos o pai.
        String[] parts = path.split("/");
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];

            // Ignora partes vazias ou referenciando o diretório atual.
            if (part.isEmpty() || part.equals(".")) {
                continue;
            }

            boolean found = false;

            // Procura a parte atual no diretório.
            for (int j = 0; j < FileSystemParam.DIR_ENTRIES; j++) {
                DirEntry entry = dirManager.readDirEntry(directoryToSearch, j);

                // Verifica se é um diretório válido e corresponde ao nome.
                if (entry != null && entry.attributes == 0x02 && new String(entry.filename).trim().equals(part)) {
                    directoryToSearch = entry.first_block; // Atualiza para o próximo bloco.
                    found = true;
                    break;
                }
            }

            // Retorna -1 se a parte não for encontrada.
            if (!found) {
                return -1;
            }
        }

        // Retorna o bloco do diretório pai.
        return directoryToSearch;
    }

    /**
     * Cria um novo diretório no caminho especificado.
     * 
     * @param path o caminho do novo diretório. Pode ser absoluto ou relativo ao diretório atual.
     */
    private void createDirectory(String path) {
        // se vazio, nada a fazer
        if (path == null || path.isEmpty()) {
            System.out.println("Usage: mkdir /path");
            return;
        }

        int parentBlock = currentDirectoryBlock; // Diretório atual 

        // Ajusta para diretório raiz se o caminho for absoluto.
        if (path.startsWith("/")) {
            parentBlock = FileSystemParam.ROOT_BLOCK;
            path = path.substring(1);
        }

        // Divide o caminho em partes e obtém o nome do diretório.
        String[] parts = path.split("/");
        String dirName = parts[parts.length - 1];

        // Percorre todas as partes do caminho, exceto a última que será criado dir.
        for (int i = 0; i < parts.length - 1; i++) {
            boolean found = false; 

            // Procura a parte atual dentro das entradas do diretório pai.
            for (int j = 0; j < FileSystemParam.DIR_ENTRIES; j++) {
                DirEntry entry = dirManager.readDirEntry(parentBlock, j); // Lê a entrada do diretório pai.

                // se é um diretório (atributo 0x02) e se o nome corresponde à parte atual.
                if (entry.attributes == 0x02 && entry.filename.toString().trim().equals(parts[i])) {
                    parentBlock = entry.first_block; // Atualiza o bloco para o próximo diretório.
                    found = true; 
                    break; 
                }
            }

            // Se a parte atual do caminho não foi encontrada, exibe erro e interrompe o processo.
            if (!found) {
                System.out.println("Error: Directory not found in path.");
                return;
            }
        }


        // Verifica se o diretório já existe.
        // HP: TODO: Isso aqui poderia ficar pra cima né ?
        for (int i = 0; i < FileSystemParam.DIR_ENTRIES; i++) {
            DirEntry entry = dirManager.readDirEntry(parentBlock, i);
            if (entry.attributes != 0 && new String(entry.filename).trim().equals(dirName)) {
                System.out.println("Error: Directory already exists.");
                return;
            }
        }

        // Cria o novo diretório.
        DirEntry newDir = new DirEntry();
        byte[] nameBytes = dirName.getBytes();
        for (int i = 0; i < nameBytes.length; i++) {
            newDir.filename[i] = nameBytes[i];
        }
        newDir.attributes = 0x02; // Marca como diretório.
        newDir.first_block = (short) fatManager.allocateBlock();
        newDir.size = 0;

        // Adiciona o novo diretório no diretório pai.
        for (int i = 0; i < FileSystemParam.DIR_ENTRIES; i++) {
            DirEntry entry = dirManager.readDirEntry(parentBlock, i);
            if (entry.attributes == 0x00) { // Encontra entrada vazia.
                dirManager.writeDirEntry(parentBlock, i, newDir);
                blockManager.initializeBlock(newDir.first_block); // Inicializa o bloco do novo diretório.
                System.out.println("Directory created: " + path);
                return;
            }
        }

        // Exibe erro se não houver espaço no diretório pai.
        System.out.println("Error: No space available in the current directory.");
    }

    /**
     * Cria um novo arquivo no caminho especificado.
     * 
     * @param path o caminho completo do arquivo a ser criado. Pode ser absoluto ou relativo ao diretório atual.
     */
    private void createFile(String path) {
        // Verifica se o caminho é nulo ou vazio.
        if (path == null || path.isEmpty()) {
            System.out.println("Usage: create /path/file");
            return;
        }

        // Obtém o bloco do diretório pai onde o arquivo será criado.
        int parentBlock = navigateToParentDirectory(path);
        if (parentBlock == -1) {
            System.out.println("Error: Directory not found.");
            return;
        }

        // Extrai o nome do arquivo do caminho.
        String fileName = extractFileName(path);
        if (fileName == null || fileName.isEmpty()) {
            System.out.println("Error: Invalid file name.");
            return;
        }

        // Verifica se o arquivo já existe no diretório pai.
        for (int i = 0; i < FileSystemParam.DIR_ENTRIES; i++) {
            DirEntry entry = dirManager.readDirEntry(parentBlock, i);
            if (entry != null && entry.attributes != 0 && new String(entry.filename).trim().equals(fileName)) {
                System.out.println("Error: File already exists.");
                return;
            }
        }

        DirEntry newFile = new DirEntry();
        byte[] nameBytes = fileName.getBytes();
        // Copia o nome do arquivo para o campo correspondente na entrada.
        System.arraycopy(nameBytes, 0, newFile.filename, 0, Math.min(nameBytes.length, newFile.filename.length));
        newFile.attributes = 0x01; // Define como arquivo regular.

        // Aloca o primeiro bloco do arquivo na FAT.
        int firstBlock = fatManager.allocateBlock();
        if (firstBlock == -1) {
            System.out.println("Error: No space left on device.");
            return;
        }
        newFile.first_block = (short) firstBlock;
        newFile.size = 0; 

        // Salva o novo arquivo no diretório pai.
        for (int i = 0; i < FileSystemParam.DIR_ENTRIES; i++) {
            DirEntry entry = dirManager.readDirEntry(parentBlock, i);
            // Encontra uma entrada vazia para registrar o novo arquivo.
            if (entry == null || entry.attributes == 0x00) {
                dirManager.writeDirEntry(parentBlock, i, newFile); // Escreve a entrada do arquivo.
                fatManager.saveFAT("filesystem.dat"); // Salva a FAT atualizada.
                System.out.println("File created: " + path);
                return;
            }
        }

        System.out.println("Error: No space available in the directory.");
    }


    /**
     * Exclui um arquivo ou diretório no caminho especificado.
     * 
     * @param path o caminho do arquivo ou diretório a ser excluído.
     *             Pode ser absoluto ou relativo ao diretório atual.
     */
    private void deleteFileOrDirectory(String path) {
        // Verifica se o caminho é nulo ou vazio.
        if (path == null || path.isEmpty()) {
            System.out.println("Usage: unlink /path/file_or_directory");
            return;
        }

        // Obtém o bloco do diretório pai do item a ser excluído.
        int parentBlock = navigateToParentDirectory(path);
        if (parentBlock == -1) {
            System.out.println("Error: Directory not found.");
            return;
        }

        // Extrai o nome do arquivo ou diretório do caminho fornecido.
        String name = extractFileName(path);
        if (name == null || name.isEmpty()) {
            System.out.println("Error: Invalid name.");
            return;
        }

        // Procura pelo item no diretório pai.
        for (int i = 0; i < FileSystemParam.DIR_ENTRIES; i++) {
            DirEntry entry = dirManager.readDirEntry(parentBlock, i);

            // Verifica se o nome corresponde e se a entrada não está vazia.
            if (entry != null && new String(entry.filename).trim().equals(name) && entry.attributes != 0x00) {
                // Se for um diretório, verifica se está vazio antes de excluí-lo.
                if (entry.attributes == 0x02) {
                    if (!isDirectoryEmpty(entry.first_block)) {
                        System.out.println("Error: Directory is not empty.");
                        return;
                    }
                }

                // Libera todos os blocos associados ao arquivo ou diretório.
                fatManager.freeChain(entry.first_block);

                // Marca a entrada como vazia.
                entry.attributes = 0x00;
                dirManager.writeDirEntry(parentBlock, i, entry);

                // Salva a FAT atualizada no disco.
                fatManager.saveFAT("filesystem.dat");

                System.out.println("Deleted: " + path);
                return;
            }
        }

        System.out.println("Error: File or directory not found.");
    }

    /**
     * Retorna se o diretório esta vazio
    */
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


    /**
     * Extrai o nome do arquivo de um caminho fornecido.
     * 
     * @param path o caminho completo do arquivo ou diretório. Pode ser absoluto ou relativo.
     * @return o nome do arquivo ou diretório como uma string, ou null se o caminho for nulo ou vazio.
     */
    private String extractFileName(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        String[] parts = path.split("/");
        return parts[parts.length - 1];
    }


    /**
     * Escreve dados em um arquivo especificado, substituindo qualquer conteúdo existente.
     * 
     * @param args argumentos no formato: "data" [rep] /path/file.
     *             - "data": Texto a ser escrito no arquivo.
     *             - [rep]: Número opcional de vezes que o texto será repetido.
     *             - /path/file: Caminho completo do arquivo onde os dados serão escritos.
     */
    private void writeToFile(String args) {
        // Verifica se os argumentos são vazios.
        if (args == null || args.isEmpty()) {
            System.out.println("Usage: write \"data\" [rep] /path/file");
            return;
        }

        // Usa regex para separar os argumentos: dados, repetições e caminho do arquivo.
        Pattern pattern = Pattern.compile("^\"([^\"]*)\"(?:\\s+(\\d+))?\\s+(.+)$");
        Matcher matcher = pattern.matcher(args);

        if (!matcher.matches()) {
            System.out.println("Usage: write \"data\" [rep] /path/file");
            return;
        }

        // Extrai os argumentos.
        String data = matcher.group(1); // Dados a serem escritos.
        String repStr = matcher.group(2); // Número de repetições (opcional).
        String path = matcher.group(3); // Caminho do arquivo.

        // Determina o número de repetições ou usa 1 como padrão.
        int rep = 1;
        if (repStr != null) {
            try {
                rep = Integer.parseInt(repStr);
            } catch (NumberFormatException e) {
                System.out.println("Error: Invalid repetition count.");
                return;
            }
        }

        // Constrói o texto completo a ser escrito, repetindo conforme necessário.
        StringBuilder dataBuilder = new StringBuilder();
        for (int i = 0; i < rep; i++) {
            dataBuilder.append(data);
        }
        String dataToWrite = dataBuilder.toString();

        // Navega até o diretório pai do arquivo.
        int parentDirectoryBlock = navigateToParentDirectory(path);
        if (parentDirectoryBlock == -1) {
            System.out.println("Error: Directory not found.");
            return;
        }

        // Extrai o nome do arquivo do caminho.
        String fileName = extractFileName(path);
        if (fileName == null || fileName.isEmpty()) {
            System.out.println("Error: Invalid file name.");
            return;
        }

        // Procura o arquivo no diretório pai.
        DirEntry entry = null;
        int entryIndex = -1;
        for (int i = 0; i < FileSystemParam.DIR_ENTRIES; i++) {
            DirEntry dirEntry = dirManager.readDirEntry(parentDirectoryBlock, i);
            if (dirEntry != null && new String(dirEntry.filename).trim().equals(fileName)
                    && dirEntry.attributes == 0x01) { // Verifica se é um arquivo.
                entry = dirEntry;
                entryIndex = i;
                break;
            }
        }

        // Se o arquivo não for encontrado, exibe erro.
        if (entry == null) {
            System.out.println("Error: File not found.");
            return;
        }

        // Libera os blocos usados anteriormente pelo arquivo.
        fatManager.freeChain(entry.first_block);

        // Converte os dados para bytes.
        byte[] dataBytes = dataToWrite.getBytes();
        int remainingData = dataBytes.length;
        int offset = 0;

        // Aloca o primeiro bloco do arquivo.
        int currentBlock = fatManager.allocateBlock();
        if (currentBlock == -1) {
            System.out.println("Error: No space left on device.");
            return;
        }
        entry.first_block = currentBlock;

        // Escreve os dados nos blocos alocados.
        while (remainingData > 0) {
            byte[] blockData = new byte[FileSystemParam.BLOCK_SIZE];
            int bytesToWrite = Math.min(FileSystemParam.BLOCK_SIZE, remainingData);
            System.arraycopy(dataBytes, offset, blockData, 0, bytesToWrite);

            // Escreve o bloco no arquivo.
            blockManager.writeBlock("filesystem.dat", currentBlock, blockData);
            remainingData -= bytesToWrite;
            offset += bytesToWrite;

            // Aloca o próximo bloco, se necessário.
            if (remainingData > 0) {
                int nextBlock = fatManager.allocateBlock();
                if (nextBlock == -1) {
                    System.out.println("Error: No space left on device.");
                    return;
                }
                fatManager.setFatValue(currentBlock, (short) nextBlock);
                currentBlock = nextBlock;
            } else {
                fatManager.setFatValue(currentBlock, (short) 0x7fff); // Marca o fim da cadeia de blocos.
            }
        }

        // Atualiza o tamanho do arquivo e salva a entrada no diretório pai.
        entry.size = dataBytes.length;
        dirManager.writeDirEntry(parentDirectoryBlock, entryIndex, entry);

        // Salva as alterações na FAT.
        fatManager.saveFAT("filesystem.dat");

        // Confirmação de que os dados foram escritos.
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

    /**
     * Lê o conteúdo de um arquivo especificado no caminho e o exibe no console.
     * 
     * @param path o caminho completo do arquivo a ser lido. Pode ser absoluto ou relativo ao diretório atual.
     */
    private void readFromFile(String path) {
        // Verifica se o caminho é vazio.
        if (path == null || path.isEmpty()) {
            System.out.println("Usage: read /path/file");
            return;
        }

        // Navega até o diretório pai do arquivo.
        int parentDirectoryBlock = navigateToParentDirectory(path);
        if (parentDirectoryBlock == -1) {
            System.out.println("Error: Directory not found.");
            return;
        }

        // Extrai o nome do arquivo pelo caminho.
        String fileName = extractFileName(path);
        if (fileName == null || fileName.isEmpty()) {
            System.out.println("Error: Invalid file name.");
            return;
        }

        // Procura a entrada correspondente ao arquivo no diretório pai.
        DirEntry entry = null;
        for (int i = 0; i < FileSystemParam.DIR_ENTRIES; i++) {
            DirEntry dirEntry = dirManager.readDirEntry(parentDirectoryBlock, i);
            if (dirEntry != null && new String(dirEntry.filename).trim().equals(fileName)
                    && dirEntry.attributes == 0x01) { // Verifica se é um arquivo.
                entry = dirEntry;
                break;
            }
        }

        if (entry == null) {
            System.out.println("Error: File not found.");
            return;
        }

        int currentBlock = entry.first_block;
        StringBuilder fileData = new StringBuilder();
        int bytesRead = 0;

        // Percorre os blocos alocados para o arquivo, seguindo a FAT.
        while (currentBlock != 0x7fff && currentBlock != 0x0000 && bytesRead < entry.size) {
            // Lê o bloco atual do arquivo.
            byte[] data = blockManager.readBlock("filesystem.dat", currentBlock);

            // Calcula quantos bytes ainda precisam ser lidos.
            int bytesToRead = Math.min(FileSystemParam.BLOCK_SIZE, entry.size - bytesRead);

            // Adiciona os dados lidos ao buffer.
            fileData.append(new String(data, 0, bytesToRead));
            bytesRead += bytesToRead;

            // Avança para o próximo bloco na FAT.
            currentBlock = fatManager.getFatValue(currentBlock);
        }

        System.out.println("Data in file: " + path + ": " + fileData.toString());
    }


    /**
     * Exibe a estrutura do diretório em forma de árvore a partir de um bloco especificado.
     * 
     * @param block o bloco inicial do diretório a ser exibido.
     * @param depth a profundidade atual na estrutura da árvore, usada para formatação visual.
     */
    private void displayTree(int block, int depth) {
        // Itera por todas as entradas do diretório.
        for (int i = 0; i < FileSystemParam.DIR_ENTRIES; i++) {
            DirEntry entry = dirManager.readDirEntry(block, i);

            // Processa apenas entradas válidas (com atributos diferentes de 0).
            if (entry.attributes != 0) {
                // Imprime a indentação correspondente à profundidade atual.
                for (int j = 0; j < depth; j++) {
                    System.out.print("  ");
                }

                // Imprime o nome da entrada, adicionando "/" no final se for um diretório.
                System.out.println("- " + new String(entry.filename).trim() +
                        (entry.attributes == 0x02 ? "/" : ""));

                // Se a entrada for um diretório, chama recursivamente para exibir seu conteúdo.
                if (entry.attributes == 0x02) {
                    displayTree(entry.first_block, depth + 1);
                }
            }
        }
    }


     /**
     * Valida se existem inconsistências nos diretórios e posteriormente arquivos
     */
    private void checkConsistency() {
        boolean[] blocksUsed = new boolean[FileSystemParam.BLOCKS];

        // marca os blocos reservados a FAT
        for (int i = 0; i < FileSystemParam.FAT_BLOCKS; i++) {
            blocksUsed[i] = true;
        }

        // Verifica se contem inconsistencia nos 
        boolean hasInconsistencies = checkDirectoryConsistency(FileSystemParam.ROOT_BLOCK, blocksUsed);

        // verifica se foram alcoados blocos não referenciados
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

    /**
     * Verifica a consistência de um diretório no sistema de arquivos.
     * 
     * @param dirBlock o bloco do diretório a ser verificado.
     * @param blocksUsed array que indica quais blocos já foram utilizados no sistema.
     * @return {@code true} se inconsistências forem encontradas, caso contrário {@code false}.
     */
    private boolean checkDirectoryConsistency(int dirBlock, boolean[] blocksUsed) {
        boolean hasInconsistencies = false; 
        blocksUsed[dirBlock] = true; 

        // Percorre todas as entradas do diretório.
        for (int i = 0; i < FileSystemParam.DIR_ENTRIES; i++) {
            DirEntry entry = dirManager.readDirEntry(dirBlock, i);

            // Verifica se a entrada é válida.
            if (entry != null && entry.attributes != 0x00) {
                int firstBlock = entry.first_block;

                // Verifica se o bloco inicial da entrada é válido.
                if (firstBlock <= 0 || firstBlock >= FileSystemParam.BLOCKS) {
                    System.out.println("Inconsistency: Entry " + new String(entry.filename).trim() + " has invalid first block.");
                    hasInconsistencies = true;
                    continue;
                }

                // Verifica se o bloco já foi utilizado.
                if (blocksUsed[firstBlock]) {
                    System.out.println("Inconsistency: Block " + firstBlock + " is already used.");
                    hasInconsistencies = true;
                    continue;
                }

                // Verifica o tipo da entrada (arquivo ou diretório) e executa a verificação correspondente.
                if (entry.attributes == 0x01) { // Arquivo
                    hasInconsistencies |= checkFileConsistency(firstBlock, blocksUsed, entry.size, entry.filename);
                } else if (entry.attributes == 0x02) { // Diretório
                    hasInconsistencies |= checkDirectoryConsistency(firstBlock, blocksUsed);
                }
            }
        }

        return hasInconsistencies; 
    }

    /**
     * Verifica a consistência de um arquivo no sistema de arquivos.
     * 
     * @param firstBlock o primeiro bloco do arquivo na FAT.
     * @param blocksUsed array que indica quais blocos já foram utilizados no sistema.
     * @param fileSize o tamanho esperado do arquivo em bytes.
     * @param filename o nome do arquivo sendo verificado.
     * @return true se inconsistências forem encontradas, caso contrário false.
     */
    private boolean checkFileConsistency(int firstBlock, boolean[] blocksUsed, int fileSize, byte[] filename) {
        boolean hasInconsistencies = false; 
        int currentBlock = firstBlock; 
        int totalSize = 0; 

        // Percorre a cadeia de blocos do arquivo.
        while (currentBlock != 0x7fff && currentBlock != 0x0000) {
            // Verifica se o bloco atual já foi usado.
            if (blocksUsed[currentBlock]) {
                System.out.println("Inconsistency: Block " + currentBlock + " is already used.");
                hasInconsistencies = true;
                break;
            }

            blocksUsed[currentBlock] = true;
            totalSize += FileSystemParam.BLOCK_SIZE;

            // Obtém o próximo bloco da cadeia a partir da FAT.
            currentBlock = fatManager.getFatValue(currentBlock);
        }

        // Verifica se a cadeia de blocos foi encerrada de forma inválida.
        if (currentBlock == 0x0000) {
            System.out.println("Inconsistency: File " + new String(filename).trim() + " chain terminated improperly.");
            hasInconsistencies = true;
        }

        // Compara o tamanho total dos blocos com o tamanho esperado do arquivo.
        if (totalSize < fileSize) {
            System.out.println("Inconsistency: File " + new String(filename).trim() + " size mismatch.");
            hasInconsistencies = true;
        }

        return hasInconsistencies; 
    }


    /**
     * Exibe o status atual dos blocos durante a execução do processo
    */
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

    /**
     * Exibe os comandos existentes
     */
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