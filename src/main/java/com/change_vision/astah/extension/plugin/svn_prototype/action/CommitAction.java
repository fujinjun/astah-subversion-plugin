package com.change_vision.astah.extension.plugin.svn_prototype.action;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import javax.swing.JOptionPane;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.auth.SVNAuthentication;
import org.tmatesoft.svn.core.auth.SVNPasswordAuthentication;
import org.tmatesoft.svn.core.auth.SVNSSHAuthentication;
import org.tmatesoft.svn.core.auth.SVNUserNameAuthentication;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.io.diff.SVNDeltaGenerator;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNWCClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import com.change_vision.astah.extension.plugin.svn_prototype.Messages;
import com.change_vision.astah.extension.plugin.svn_prototype.dialog.SVNCommitCommentDialog;
import com.change_vision.astah.extension.plugin.svn_prototype.util.SVNUtils;
import com.change_vision.jude.api.inf.exception.ProjectNotFoundException;
import com.change_vision.jude.api.inf.project.ProjectAccessor;
import com.change_vision.jude.api.inf.project.ProjectAccessorFactory;
import com.change_vision.jude.api.inf.ui.IPluginActionDelegate;
import com.change_vision.jude.api.inf.ui.IWindow;

public class CommitAction implements IPluginActionDelegate {

    private final String SVN_SEPARATOR = "/";
    private final String BASE_REVISION_REGEX = "%%%baseRevision%%%";
    private final String LATEST_REVISION_REGEX = "%%%latestRevision%%%";

    @Override
    public Object run(IWindow arg0) throws UnExpectedException {
        try {
            // 開いているプロジェクトのパスを取得
//            ProjectAccessor projectAccessor = ProjectAccessorFactory.getProjectAccessor();
//            String pjPath = projectAccessor.getProjectPath();
//
//            if (SVNUtils.chkNotSaveProject(pjPath)) {
//                return null;
//            }
//
//            if (SVNUtils.chkEditingProject()){
//                JOptionPane.showMessageDialog(null, Messages.getMessage("confirm_save_dialog.message"));
//                return null;
//            }
            // 開いているプロジェクトのパスを取得
            String pjPath = getOpenProjectPath();

            if (pjPath == null) {
                return null;
            }

            long latestRevision = 0;
            long baseRevision   = 0;

            String fileName = getFileName(pjPath);

            // SVNKitの初期化
            initializeSVNKit();

            // 保存してあるSubversionログイン情報取得
            SVNUtils utils = getSVNUtils();

            latestRevision = getLatestRevision(fileName, utils);
            baseRevision = getBaseRevision(new File(pjPath));

            // コミット対象のファイルが新規ではなく古い場合
            if (checkConflict(baseRevision, latestRevision)) {
                return null;
            }

            // コミットコメント
            String comment = displayCommitCommentWindow();
            if (comment == null) {
                return null;
            }
//            SVNCommitCommentDialog commentDialog = new SVNCommitCommentDialog((SVNUtils.getViewManager()).getMainFrame());
//            commentDialog.setVisible(true);
//            if (!commentDialog.getRunCommitFlg()){
//                JOptionPane.showMessageDialog(null, Messages.getMessage("info_message.commit_cancel"));
//                return null;
//            }

            boolean result = false;
            if (baseRevision < 0) {
                // 新規登録
                result = newRegistration(pjPath, fileName, comment, utils);
////                ISVNEditor editor = (utils.repos).getCommitEditor(commentDialog.getCommitComment(), null, true, null);
//                ISVNEditor editor = (utils.repos).getCommitEditor(comment, null, true, null);
//                editor.openRoot(-1);
//                editor.addFile(fileName, null,-1);
//                editor.applyTextDelta(fileName, null);
//                SVNDeltaGenerator deltaGenerator = new SVNDeltaGenerator();
//                InputStream is = new FileInputStream(new File(pjPath));
//                String checksum = deltaGenerator.sendDelta(fileName, is, editor, true);
//
//                editor.closeFile(fileName, checksum);
//                editor.closeDir();
//
//                editor.closeEdit();
            } else {
                // 登録済みファイルUPDATE コミット 
                // 古いコンテンツを取得する 
                byte[] oldData;
                byte[] newData;
                ByteArrayInputStream oldStream = null;
                ByteArrayInputStream newStream = null;

                String checksum;

                ISVNEditor editor = null;
                SVNDirEntry entry = (utils.repos).info(fileName, -1);
                SVNDeltaGenerator deltaGenerator = new SVNDeltaGenerator();

                newData = SVNUtils.readFileByte(pjPath);
                newStream = new ByteArrayInputStream(newData);

                if (entry != null) {
                    ByteArrayOutputStream oldOut = new ByteArrayOutputStream();

                    (utils.repos).getFile(fileName, -1, SVNProperties.wrap(Collections.EMPTY_MAP), oldOut);
//                    editor = (utils.repos).getCommitEditor(commentDialog.getCommitComment(), null, true, null);
                    editor = (utils.repos).getCommitEditor(comment, null, true, null);
                    editor.openRoot(-1);
                    oldData = oldOut.toByteArray();
                    oldStream = new ByteArrayInputStream(oldData);
                    editor.openFile(fileName, -1);
                    editor.applyTextDelta(fileName, null);
                    checksum = deltaGenerator.sendDelta(fileName, oldStream, 0, newStream, editor, true);
                } else {
//                    editor = (utils.repos).getCommitEditor(commentDialog.getCommitComment(), null, true, null);
                    editor = (utils.repos).getCommitEditor(comment, null, true, null);
                    editor.openRoot(-1);
                    editor.addFile(fileName, null,-1);
                    editor.applyTextDelta(fileName, null);
                    InputStream is = new FileInputStream(new File(pjPath));
                    checksum = deltaGenerator.sendDelta(fileName, is, editor, true);
                }

                editor.closeFile(fileName, checksum);
                editor.closeDir();
                editor.closeEdit();
                editor = null;
            }

            long revision = (utils.repos).getLatestRevision(); 
            File originFile = new File(pjPath + ".r" + revision);

            // ファイルを更新
            UpdateAction.doUpdate(arg0, utils, pjPath);

            if (originFile.exists()) {
                // 競合した場合
                SVNClientManager scm;
                if (SVNUtils.chkNullString(utils.password)){
                    scm = SVNClientManager.newInstance(SVNWCUtil.createDefaultOptions(true), utils.getAuthManager());
                } else {
                    scm = SVNClientManager.newInstance(SVNWCUtil.createDefaultOptions(true), utils.user, utils.password);
                }
                // 競合解消のため、「元に戻す」処理を実行
                SVNWCClient wcClient = scm.getWCClient();
                wcClient.doRevert(new File[]{new File(pjPath)}, SVNDepth.INFINITY, null);
            }

            JOptionPane.showMessageDialog(null, Messages.getMessage("info_message.commit_complete"));
        } catch (SVNException se){
            if (!SVNUtils.chkLoginError(se)){
                JOptionPane.showMessageDialog(null, Messages.getMessage("err_message.common_svn_error"));
            }
//        } catch (ClassNotFoundException cfe) {
//            JOptionPane.showMessageDialog(null, Messages.getMessage("err_message.common_class_not_found"));
//        } catch (ProjectNotFoundException cfe) {
//            JOptionPane.showMessageDialog(null, Messages.getMessage("err_message.common_not_open_project"));
        } catch (IOException ie) {
            JOptionPane.showMessageDialog(null, Messages.getMessage("err_message.common_io_error"));
        }
        return null;
    }

    private long getBaseRevision(File destination) throws SVNException{
        SVNClientManager clientManager = SVNClientManager.newInstance();
        return clientManager.getStatusClient().doStatus(destination, false).getRevision().getNumber();
    }

    public String getOpenProjectPath() {
        // 開いているプロジェクトのパスを取得
        ProjectAccessor projectAccessor;
        String pjPath = null;

        try {
            projectAccessor = ProjectAccessorFactory.getProjectAccessor();
            pjPath = projectAccessor.getProjectPath();

            if (SVNUtils.chkNotSaveProject(pjPath)) {
                return null;
            }

            if (SVNUtils.chkEditingProject()){
                JOptionPane.showMessageDialog(null, Messages.getMessage("confirm_save_dialog.message"));
                return null;
            }
        } catch (ClassNotFoundException e) {
            JOptionPane.showMessageDialog(null, Messages.getMessage("err_message.common_class_not_found"));
            return null;
        } catch (ProjectNotFoundException e) {
            JOptionPane.showMessageDialog(null, Messages.getMessage("err_message.common_not_open_project"));
            return null;
        }
        return pjPath;
    }

    public String getFileName(String pjPath) {
        int markIndex = pjPath.lastIndexOf(File.separator);
        String fileName = pjPath.substring(markIndex + 1);

        return fileName;
    }

    public void initializeSVNKit() {
        // SVNKitの初期化
        DAVRepositoryFactory.setup();
        SVNRepositoryFactoryImpl.setup();
        FSRepositoryFactory.setup();
    }

    public SVNUtils getSVNUtils() {
        // 保存してあるSubversionログイン情報取得
        SVNUtils utils = new SVNUtils();
        if (!utils.getPreferencesInfo(Messages.getMessage("info_message.commit_cancel"))){
            return null;
        }
        return utils;
    }

    public long getLatestRevision(String fileName, SVNUtils utils) {
        long latestRevision = 0;

        String fileURL = utils.repository + SVN_SEPARATOR + fileName;
        if (fileURL.matches("^/.*")){
            fileURL = fileURL.substring(1);
        }
        SVNRepository latestRepos;
        try {
            latestRepos = SVNRepositoryFactory.create(SVNURL.parseURIDecoded(fileURL));

            ISVNAuthenticationManager authManager;

            if (utils.loginKind == SVNUtils.LOGIN_KIND_SSH && SVNUtils.chkNullString(utils.password)){
                authManager = new BasicAuthenticationManager(new SVNAuthentication[] {new SVNUserNameAuthentication(utils.user, false, SVNURL.parseURIEncoded(utils.repository), false),
                                                                                      new SVNSSHAuthentication(utils.user, new File(utils.keyFilePath), null, -1, false, SVNURL.parseURIEncoded(utils.repository), false)}
                                                            );
            } else {
                authManager = new BasicAuthenticationManager(new SVNAuthentication[] {new SVNUserNameAuthentication(utils.user, false, SVNURL.parseURIEncoded(utils.repository), false),
                                                                                      new SVNPasswordAuthentication(utils.user, utils.password, false, SVNURL.parseURIEncoded(utils.repository), false)}
                                                            );
            }
            latestRepos.setAuthenticationManager(authManager);

            latestRevision = latestRepos.getLatestRevision();
        } catch (SVNException e) {
            if (!SVNUtils.chkLoginError(e)){
                JOptionPane.showMessageDialog(null, Messages.getMessage("err_message.common_svn_error"));
            }
            return -1;
        }
        return latestRevision;
    }

    public boolean checkConflict(long baseRevision, long latestRevision) {
        // コミット対象のファイルが新規ではなく古い場合
        if (baseRevision > 0 && latestRevision != baseRevision){
            String conflictMessage = Messages.getMessage("info_message.commit_conflict");
            conflictMessage = conflictMessage.replace(BASE_REVISION_REGEX, String.valueOf(baseRevision));
            conflictMessage = conflictMessage.replace(LATEST_REVISION_REGEX, String.valueOf(latestRevision));
            JOptionPane.showMessageDialog(null, conflictMessage);
            return true;
        }
        return false;
    }

    public String displayCommitCommentWindow() {
        // コミットコメント
        String comment = "";
        SVNCommitCommentDialog commentDialog = new SVNCommitCommentDialog((SVNUtils.getViewManager()).getMainFrame());
        commentDialog.setVisible(true);
        if (!commentDialog.getRunCommitFlg()){
            JOptionPane.showMessageDialog(null, Messages.getMessage("info_message.commit_cancel"));
            return null;
        }
        comment = commentDialog.getCommitComment();
        if (comment == null) {
            return "";
        }
        return comment;
    }

    public boolean newRegistration(String pjPath, String fileName, String comment, SVNUtils utils) {
        // 新規登録
        ISVNEditor editor;
        try {
            editor = (utils.repos).getCommitEditor(comment, null, true, null);
            editor.openRoot(-1);
            editor.addFile(fileName, null,-1);
            editor.applyTextDelta(fileName, null);
            SVNDeltaGenerator deltaGenerator = new SVNDeltaGenerator();
            InputStream is;
            is = new FileInputStream(new File(pjPath));
            String checksum = deltaGenerator.sendDelta(fileName, is, editor, true);

            editor.closeFile(fileName, checksum);
            editor.closeDir();

            editor.closeEdit();
        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(null, Messages.getMessage("err_message.common_io_error"));
            return false;
        } catch (SVNException e) {
            if (!SVNUtils.chkLoginError(e)){
                JOptionPane.showMessageDialog(null, Messages.getMessage("err_message.common_svn_error"));
            }
            return false;
        }
        return true;
    }
}
