package com.change_vision.astah.extension.plugin.svn_prototype;

import java.io.BufferedReader;
import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.SwingWorker;

//import org.tmatesoft.svn.core.SVNDepth;
//import org.tmatesoft.svn.core.SVNException;
//import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
//import org.tmatesoft.svn.core.wc.ISVNOptions;
//import org.tmatesoft.svn.core.wc.SVNDiffClient;
//import org.tmatesoft.svn.core.wc.SVNRevision;

import com.change_vision.astah.extension.plugin.svn_prototype.util.SVNPreferences;
//import com.change_vision.astah.extension.plugin.svn_prototype.util.SVNUtils;

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
            SVNPreferences.getInstace(this.getClass());
            // Preferences のインスタンスを取得
            Preferences preferences = SVNPreferences.getInstance();
            String commandPath = preferences.get(SVNPreferences.KEY_ASTAH_HOME, null);

            String commandExtension = ".sh";
            String os = System.getProperty("os.name");

            if (os.matches("^Windows.*")) {
                //commandExtension = ".bat";
                commandExtension = "w.exe";
            }
            String command = commandPath + File.separator + "astah-command" + commandExtension;

            String[] diffCommand = new String[]{command, "-diff", oldFile, newFile};

            Runtime r = Runtime.getRuntime();
            Process p = r.exec(diffCommand);

            InputStream  pis;
            OutputStream pos;
            InputStream  pes;
            String processResult = "";
            String resultLine = "";
            InputStreamReader isr;
            BufferedReader br;

            // プロセス内部で開かれているストリームを閉じる
            pis = p.getInputStream();
            pos = p.getOutputStream();
            pes = p.getErrorStream();

            isr = new InputStreamReader(pes);
            br  = new BufferedReader(isr);

            while ((resultLine = br.readLine()) != null) {
                processResult = processResult + resultLine + "\n";
            }

            processResult = processResult + "\n\n\n";
            resultLine = "";

            isr = new InputStreamReader(pis);
            br  = new BufferedReader(isr);

            while ((resultLine = br.readLine()) != null) {
                processResult = processResult + resultLine + "\n";
            }

//            if (!SVNUtils.chkNullString(processResult)){
//                System.out.println(processResult);
//            }

            p.waitFor();

            setProgress(100);

            pis.close();
            pos.close();
            pes.close();
            p.destroy();
            p = null;
            r.gc();
            finishFlg = true;
        } catch(IOException ie) {
            ie.printStackTrace();
        } catch(InterruptedException ine) {
            ine.printStackTrace();
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

//    public static boolean chkSameFile(String fileName1, String fileName2, ISVNAuthenticationManager authManager, ISVNOptions options){
//    	try {
//    	    SVNDiffClient diffClient = new SVNDiffClient(authManager, options);
//    	    FileOutputStream os = new FileOutputStream("C:\\diffResult.txt");
//    	    diffClient.doDiff(new File(fileName1), SVNRevision.WORKING, new File(fileName2), SVNRevision.COMMITTED, SVNDepth.INFINITY, true, os, null);
//    	    os.close();
//    	} catch (SVNException se) {
//    		se.printStackTrace();
//    	} catch (FileNotFoundException fnfe) {
//			fnfe.printStackTrace();
//		} catch (IOException ie) {
//			ie.printStackTrace();
//		}
//    	return false;
//    }
}
