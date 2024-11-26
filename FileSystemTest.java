import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class FileSystemTest {

    public static void main(String[] args) {
        FileSystemTest test = new FileSystemTest();
        test.runAllTests();
    }

    public void runAllTests() {
        testInit();
        testCreateDirectory();
        testCreateFile();
        testWriteToFile();
        testAppendToFile();
        testReadFromFile();
        testDeleteFile();
        testDeleteDirectory();
        testChangeDirectory();
        testListDirectory();
        testDisplayTree();
        testCheckConsistency();
        testShowStats();
        testHelpCommand();
        System.out.println("All tests completed.");
    }

    private String executeCommandAndGetOutput(FileSystemShell shell, String command) {
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        shell.executeCommand(command);

        System.out.flush();
        System.setOut(originalOut);
        return outContent.toString();
    }

    private void testInit() {
        System.out.println("Testing 'init' command...");
        FileSystemShell shell = new FileSystemShell();
        String output = executeCommandAndGetOutput(shell, "init");
        if (output.contains("FileSystem initialized.")) {
            System.out.println("FileSystem initialized successfully.");
        } else {
            System.out.println("FileSystem initialization failed.");
        }
    }

    private void testCreateDirectory() {
        System.out.println("Testing 'mkdir' command...");
        FileSystemShell shell = new FileSystemShell();
        shell.executeCommand("init");

        String output = executeCommandAndGetOutput(shell, "mkdir /testdir");
        if (output.contains("Directory created: /testdir")) {
            System.out.println("Directory creation successful.");
        } else {
            System.out.println("Directory creation failed.");
        }

        output = executeCommandAndGetOutput(shell, "ls /");
        if (output.contains("Directory: testdir")) {
            System.out.println("Directory listed in ls command.");
        } else {
            System.out.println("Directory not found in ls command.");
        }
    }

    private void testCreateFile() {
        System.out.println("Testing 'create' command...");
        FileSystemShell shell = new FileSystemShell();
        shell.executeCommand("init");

        String output = executeCommandAndGetOutput(shell, "create /testfile");
        if (output.contains("File created: /testfile")) {
            System.out.println("File creation successful.");
        } else {
            System.out.println("File creation failed.");
        }

        output = executeCommandAndGetOutput(shell, "ls /");
        if (output.contains("File: testfile")) {
            System.out.println("File listed in ls command.");
        } else {
            System.out.println("File not found in ls command.");
        }
    }

    private void testWriteToFile() {
        System.out.println("Testing 'write' command...");
        FileSystemShell shell = new FileSystemShell();
        shell.executeCommand("init");
        shell.executeCommand("create /testfile");

        String output = executeCommandAndGetOutput(shell, "write \"Hello World!\" 5 /testfile");
        if (output.contains("Data written to file: /testfile")) {
            System.out.println("Data written successfully.");
        } else {
            System.out.println("Data writing failed.");
        }

        output = executeCommandAndGetOutput(shell, "read /testfile");
        if (output.contains("Hello World!Hello World!Hello World!Hello World!Hello World!")) {
            System.out.println("Data read matches written data.");
        } else {
            System.out.println("Data read does not match written data.");
        }
    }

    private void testAppendToFile() {
        System.out.println("Testing 'append' command...");
        FileSystemShell shell = new FileSystemShell();
        shell.executeCommand("init");
        shell.executeCommand("create /testfile");
        shell.executeCommand("write \"Hello\" 1 /testfile");

        String output = executeCommandAndGetOutput(shell, "append \" World!\" 1 /testfile");
        if (output.contains("Data appended to file: /testfile")) {
            System.out.println("Data appended successfully.");
        } else {
            System.out.println("Data appending failed.");
        }

        output = executeCommandAndGetOutput(shell, "read /testfile");
        if (output.contains("Hello World!")) {
            System.out.println("Data read matches appended data.");
        } else {
            System.out.println("Data read does not match appended data.");
        }
    }

    private void testReadFromFile() {
        System.out.println("Testing 'read' command...");
        FileSystemShell shell = new FileSystemShell();
        shell.executeCommand("init");
        shell.executeCommand("create /testfile");
        shell.executeCommand("write \"Testing read function.\" 1 /testfile");

        String output = executeCommandAndGetOutput(shell, "read /testfile");
        if (output.contains("Testing read function.")) {
            System.out.println("Read command works correctly.");
        } else {
            System.out.println("Read command failed.");
        }
    }

    private void testDeleteFile() {
        System.out.println("Testing 'unlink' command for files...");
        FileSystemShell shell = new FileSystemShell();
        shell.executeCommand("init");
        shell.executeCommand("create /testfile");

        String output = executeCommandAndGetOutput(shell, "unlink /testfile");
        if (output.contains("Deleted: /testfile")) {
            System.out.println("File deleted successfully.");
        } else {
            System.out.println("File deletion failed.");
        }

        output = executeCommandAndGetOutput(shell, "ls /");
        if (!output.contains("File: testfile")) {
            System.out.println("File no longer listed in ls command.");
        } else {
            System.out.println("File still listed in ls command.");
        }
    }

    private void testDeleteDirectory() {
        System.out.println("Testing 'unlink' command for directories...");
        FileSystemShell shell = new FileSystemShell();
        shell.executeCommand("init");
        shell.executeCommand("mkdir /testdir");

        String output = executeCommandAndGetOutput(shell, "unlink /testdir");
        if (output.contains("Deleted: /testdir")) {
            System.out.println("Directory deleted successfully.");
        } else {
            System.out.println("Directory deletion failed.");
        }

        output = executeCommandAndGetOutput(shell, "ls /");
        if (!output.contains("Directory: testdir")) {
            System.out.println("Directory no longer listed in ls command.");
        } else {
            System.out.println("Directory still listed in ls command.");
        }
    }

    private void testChangeDirectory() {
        System.out.println("Testing 'cd' command...");
        FileSystemShell shell = new FileSystemShell();
        shell.executeCommand("init");
        shell.executeCommand("mkdir /testdir");
        String output = executeCommandAndGetOutput(shell, "cd /testdir");
        if (output.contains("Changed to directory: /testdir")) {
            System.out.println("Changed directory successfully.");
        } else {
            System.out.println("Change directory failed.");
        }
        // Create a file in the current directory
        shell.executeCommand("create testfile");
        output = executeCommandAndGetOutput(shell, "ls");
        if (output.contains("File: testfile")) {
            System.out.println("File created in new directory.");
        } else {
            System.out.println("File not found in new directory.");
        }
    }

    private void testListDirectory() {
        System.out.println("Testing 'ls' command...");
        FileSystemShell shell = new FileSystemShell();
        shell.executeCommand("init");
        shell.executeCommand("mkdir /dir1");
        shell.executeCommand("mkdir /dir2");
        shell.executeCommand("create /file1");
        String output = executeCommandAndGetOutput(shell, "ls /");
        if (output.contains("Directory: dir1") && output.contains("Directory: dir2") && output.contains("File: file1")) {
            System.out.println("ls command lists directories and files correctly.");
        } else {
            System.out.println("ls command failed to list directories and files correctly.");
        }
    }

    private void testDisplayTree() {
        System.out.println("Testing 'tree' command...");
        FileSystemShell shell = new FileSystemShell();
        shell.executeCommand("init");
        shell.executeCommand("mkdir /dir1");
        shell.executeCommand("mkdir /dir1/subdir1");
        shell.executeCommand("create /dir1/subdir1/file1");
        String output = executeCommandAndGetOutput(shell, "tree");
        if (output.contains("- dir1/") && output.contains("- subdir1/") && output.contains("- file1")) {
            System.out.println("tree command displays directory structure correctly.");
        } else {
            System.out.println("tree command failed to display directory structure correctly.");
        }
    }

    private void testCheckConsistency() {
        System.out.println("Testing 'check' command...");
        FileSystemShell shell = new FileSystemShell();
        shell.executeCommand("init");
        String output = executeCommandAndGetOutput(shell, "check");
        if (output.contains("FileSystem is consistent.")) {
            System.out.println("FileSystem consistency check passed.");
        } else {
            System.out.println("FileSystem consistency check failed.");
        }
    }

    private void testShowStats() {
        System.out.println("Testing 'stats' command...");
        FileSystemShell shell = new FileSystemShell();
        shell.executeCommand("init");
        String output = executeCommandAndGetOutput(shell, "stats");
        if (output.contains("FileSystem Stats:")) {
            System.out.println("Stats command displays file system statistics.");
        } else {
            System.out.println("Stats command failed.");
        }
    }

    private void testHelpCommand() {
        System.out.println("Testing 'help' command...");
        FileSystemShell shell = new FileSystemShell();
        String output = executeCommandAndGetOutput(shell, "help");
        if (output.contains("Available commands:")) {
            System.out.println("Help command displays available commands.");
        } else {
            System.out.println("Help command failed.");
        }
    }
}
