package com.change_vision.astah.extension.plugin.svn_prototype;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import com.change_vision.astah.extension.plugin.svn_prototype.util.SVNPreferences;
//import com.change_vision.astah.extension.plugin.svn_prototype.util.SVNUtils;
import com.change_vision.astah.extension.plugin.svn_prototype.util.SvnDiffInputStreamThread;

public class SVNDiffTask extends SwingWorker<List<Integer>, Integer> {

    private String  oldFile;
    private String  newFile;
    private boolean finishFlg;
    private boolean newFileDeleteFlg;

    public SVNDiffTask(String file1, String file2){
        oldFile   = file1;
        newFile   = file2;
        finishFlg = false;
        newFileDeleteFlg = false;
    }

    public SVNDiffTask(String file1, String file2, boolean newFileDeleteFlg){
        oldFile   = file1;
        newFile   = file2;
        finishFlg = false;
        this.newFileDeleteFlg = newFileDeleteFlg;
    }

    @Override
    protected List<Integer> doInBackground() {
        try {
            boolean directFlg = false;
            SVNPreferences.getInstace(this.getClass());
            // Preferences のインスタンスを取得
            Preferences preferences = SVNPreferences.getInstance();
            String commandPath = preferences.get(SVNPreferences.KEY_ASTAH_HOME, null);
            String currentDir = new File(".").getAbsoluteFile().getParent();

            String os = System.getProperty("os.name");

            String[] diffCommand = null;

            if (os.matches("^Mac.*")) {
                if (commandPath.equals(currentDir)){
                    // カレントディレクトリと保存してあるastahインストールディレクトリが同じ場合
                    commandPath = "." + File.separator;
                } else {
                    // カレントディレクトリと保存してあるastahインストールディレクトリが同じ場合
//                    String newCmdPath = new String();
//                    String separator = File.separator;
//                    String[] splitPath = currentDir.split(separator);
//                    int separatorNum = splitPath.length;
//
//                    for (int i = 0; i < separatorNum; i++) {
//                        newCmdPath = newCmdPath + ".."  +File.separator;
//                    }
//
//                    if (commandPath.startsWith(File.separator)){
//                        commandPath = newCmdPath + commandPath.substring(1);
//                    } else {
//                        commandPath = newCmdPath + commandPath;
//                    }
                    if (!commandPath.endsWith(File.separator)){
                        commandPath = commandPath + File.separator;
                    }
                    // java
                	// "-Xms64m -Xmx1024m"
                	// -cp
                	// "`dirname astah-command.sh`"/astah professional.app/Contents/Resources/Java"/astah-pro.jar"
                	// com.change_vision.jude.cmdline.JudeCommandRunner
                	// "-diff C:\Documents and Settings\kasaba\デスクトップ\sample_checkout2-2\class8.asta C:\Documents and Settings\kasaba\デスクトップ\sample_checkout2-2\latest.class8.asta"
                	directFlg = true;
                	diffCommand = new String[]{"java",
                			                   "-Xms64m",
                			                   "-Xmx1024m",
                			                   "-cp",
                			                   commandPath + "astah professional.app/Contents/Resources/Java/astah-pro.jar",
                			                   "com.change_vision.jude.cmdline.JudeCommandRunner",
                			                   "-diff",
                			                   oldFile,
                			                   newFile};
                }
            } else if (os.matches("^Windows.*")) {
                if (!commandPath.endsWith(File.separator)){
                    commandPath = commandPath + File.separator;
                }

                directFlg = true;
            	diffCommand = new String[]{"java",
            			                   "-Xms16m",
            			                   "-Xmx384m",
//		                                   "-Xms64m",
//		                                   "-Xmx1024m",
            			                   "-Dsun.java2d.noddraw=true",
            			                   "-cp",
            			                   commandPath + "astah-pro.jar",
            			                   "com.change_vision.jude.cmdline.JudeCommandRunner",
            			                   "-diff",
            			                   oldFile,
            			                   newFile};
            }
//            if (os.matches("^Linux.*")) {
//                if (!commandPath.endsWith(File.separator)){
//                    commandPath = commandPath + File.separator;
//                }
//            	directFlg = true;
//            	diffCommand = new String[]{"java",
//            			                   "-Xms64m",
//            			                   "-Xmx1024m",
//            			                   "-cp",
//            			                   commandPath + "astah-pro.jar",
//            			                   "com.change_vision.jude.cmdline.JudeCommandRunner",
//            			                   "-diff",
//            			                   oldFile,
//            			                   newFile};
//            }
//            System.out.println("java " +
//	                           "-Xms64m -Xmx1024m " +
//	                           "-cp " +
//	                           "\"" + commandPath + "astah-pro.jar\" " +
//	                           "com.change_vision.jude.cmdline.JudeCommandRunner " +
//	                           "-diff " +
//	                           oldFile + " " +
//	                           newFile);

            if (!directFlg){
                String commandExtension = ".sh";

                if (os.matches("^Windows.*")) {
                    commandExtension = "w.exe";
//            } else {
//                commandPath = SVNUtils.escapeSpaceForMac(commandPath);
//                oldFile     = SVNUtils.escapeSpaceForMac(oldFile);
//                newFile     = SVNUtils.escapeSpaceForMac(newFile);
                }

                String command;
                if (commandPath.endsWith(File.separator)){
                    command = commandPath;
                } else {
                    command = commandPath + File.separator;
                }
                command = command + "astah-command" + commandExtension;
//            String escCom = SVNUtils.escapeSpaceForMac(command);

                diffCommand = new String[]{command, "-diff", oldFile, newFile};
            }

//            System.out.println("---------- Command ----------");
//            for (int i = 0; i < diffCommand.length; i++) {
//                System.out.println(i + ":" + diffCommand[i]);
//            }
//            System.out.println("---------- Command ----------");
            Runtime r = Runtime.getRuntime();
            Process p = r.exec(diffCommand);

            SvnDiffInputStreamThread  pis;
            SvnDiffInputStreamThread  pes;
            String processResult = "";
            String resultLine = "";
            InputStreamReader isr;
            BufferedReader br;

            // プロセス内部で開かれているストリームを閉じる
            pis = new SvnDiffInputStreamThread(p.getInputStream());
            pes = new SvnDiffInputStreamThread(p.getErrorStream());
            pis.start();
            pes.start();

//            isr = new InputStreamReader(pes);
//            br  = new BufferedReader(isr);
//
//            while ((resultLine = br.readLine()) != null) {
//                processResult = processResult + resultLine + "\n";
//            }
//
//            processResult = processResult + "\n\n\n";
//            resultLine = "";
//
//            isr = new InputStreamReader(pis);
//            br  = new BufferedReader(isr);
//
//            while ((resultLine = br.readLine()) != null) {
//                processResult = processResult + resultLine + "\n";
//            }

            // プロセスの終了待ち
            p.waitFor();

            // InputStreamスレッドの終了待ち
            pis.join();
            pes.join();

            setProgress(100);

//            System.out.println(processResult);
//            pis.close();
//            pes.close();
            p.destroy();
            p = null;
            r.gc();
            finishFlg = true;
        } catch(IOException ie) {
//            ie.printStackTrace();
            JOptionPane.showMessageDialog(null, Messages.getMessage("err_message.common_exception_from_commandline_tool"));
        } catch(InterruptedException ine) {
//            ine.printStackTrace();
            JOptionPane.showMessageDialog(null, Messages.getMessage("err_message.common_exception_from_commandline_tool"));
        }
        return null;
    }

    @Override
    protected void done() {
        if (newFileDeleteFlg){
            // 表示後は、比較対象のファイルを削除
            File file = new File(newFile);
            file.delete();
        }
    }

    public boolean getFinishFlg() {
        return finishFlg;
    }

    public void resetFinishFlg() {
        finishFlg = false;
    }
}
