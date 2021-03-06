package com.change_vision.astah.extension.plugin.svn_prototype.task;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.prefs.Preferences;

import com.change_vision.astah.extension.plugin.svn_prototype.Messages;
import com.change_vision.astah.extension.plugin.svn_prototype.exception.SVNPluginException;
import com.change_vision.astah.extension.plugin.svn_prototype.util.SVNPreferences;
import com.change_vision.astah.extension.plugin.svn_prototype.util.SVNUtils;
import com.change_vision.astah.extension.plugin.svn_prototype.util.SvnDiffInputStreamThread;

public class SVNDiffTask {

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

    public List<Integer> doInBackground() throws SVNPluginException, ClassNotFoundException {
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
                    if (!commandPath.endsWith(File.separator)){
                        commandPath = commandPath + File.separator;
                    }
                    String jarPath = (new SVNUtils()).getMacAstahPath();
                    directFlg = true;
                    diffCommand = new String[]{"java",
                                               "-Xms64m",
                                               "-Xmx1024m",
                                               "-cp",
                                               jarPath,
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
                                           "-Dsun.java2d.noddraw=true",
                                           "-cp",
                                           commandPath + "astah-pro.jar",
                                           "com.change_vision.jude.cmdline.JudeCommandRunner",
                                           "-diff",
                                           oldFile,
                                           newFile};
            }

            if (!directFlg){
                String commandExtension = ".sh";

                if (os.matches("^Windows.*")) {
                    commandExtension = "w.exe";
                }

                String command;
                if (commandPath.endsWith(File.separator)){
                    command = commandPath;
                } else {
                    command = commandPath + File.separator;
                }
                command = command + "astah-command" + commandExtension;

                diffCommand = new String[]{command, "-diff", oldFile, newFile};
            }

            Runtime r = Runtime.getRuntime();
            Process p = r.exec(diffCommand);

            SvnDiffInputStreamThread  pis;
            SvnDiffInputStreamThread  pes;

            // 別スレッドでストリームの内容を読み出し
            pis = new SvnDiffInputStreamThread(p.getInputStream());
            pes = new SvnDiffInputStreamThread(p.getErrorStream());
            pis.start();
            pes.start();


            // プロセスの終了待ち
            p.waitFor();

            // InputStreamスレッドの終了待ち
            pis.join();
            pes.join();

            p.destroy();
            p = null;
            r.gc();
            finishFlg = true;
        } catch(IOException ie) {
            throw new SVNPluginException(Messages.getMessage("err_message.common_exception_from_commandline_tool"), ie);
        } catch(InterruptedException ine) {
            throw new SVNPluginException(Messages.getMessage("err_message.common_exception_from_commandline_tool"), ine);
        }
        return null;
    }

    public void done() {
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
