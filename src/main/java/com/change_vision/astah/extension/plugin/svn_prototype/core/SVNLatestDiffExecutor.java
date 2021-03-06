package com.change_vision.astah.extension.plugin.svn_prototype.core;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.FileNotFoundException;

import org.tmatesoft.svn.core.SVNException;

import com.change_vision.astah.extension.plugin.svn_prototype.Messages;
import com.change_vision.astah.extension.plugin.svn_prototype.dialog.SVNProgressDialog;
import com.change_vision.astah.extension.plugin.svn_prototype.exception.SVNNotCommitException;
import com.change_vision.astah.extension.plugin.svn_prototype.exception.SVNNotConfigurationException;
import com.change_vision.astah.extension.plugin.svn_prototype.exception.SVNPluginException;
import com.change_vision.astah.extension.plugin.svn_prototype.ui.swing.SVNDiffSwingWorker;
import com.change_vision.astah.extension.plugin.svn_prototype.util.ISVNKitUtils;
import com.change_vision.astah.extension.plugin.svn_prototype.util.SVNUtils;
import com.change_vision.jude.api.inf.exception.ProjectNotFoundException;
import com.change_vision.jude.api.inf.project.ProjectAccessor;
import com.change_vision.jude.api.inf.ui.IPluginActionDelegate.UnExpectedException;

public class SVNLatestDiffExecutor {

    private ProjectAccessor projectAccessor;
    private SVNUtils utils;
    private ISVNKitUtils kitUtils;

    public SVNLatestDiffExecutor(ProjectAccessor projectAccessor, ISVNKitUtils kitUtils) {
        super();
        this.projectAccessor = projectAccessor;
        this.utils = new SVNUtils();
        this.kitUtils = kitUtils;
        this.utils.setSVNKitUtils(this.kitUtils);
    }

    public void setUtils(SVNUtils utils) {
        this.utils = utils;
    }

    public Object execute() throws UnExpectedException, SVNPluginException,
                                   SVNNotConfigurationException, SVNNotCommitException,
                                   ProjectNotFoundException, SVNException,
                                   FileNotFoundException {
        // 開いているプロジェクトのパスを取得
        String pjPath = utils.getProjectPath(projectAccessor);
        String fileName = SVNUtils.getFileName(pjPath);
        String filePath = SVNUtils.getFilePath(pjPath);

        // SVNKitの初期化
        initializeSVN();

        // リポジトリから最新リビジョンのファイルを取得
        String workFile = kitUtils.getLatestFile(filePath, fileName);

        // 差分表示
        displayDiff(pjPath, workFile);

        return null;
    }

    public void initializeSVN() throws SVNNotConfigurationException, SVNPluginException {

        // SVNKitの初期化
        kitUtils.initialize();

        // 保存してあるSubversionログイン情報取得
        if (!utils.getPreferencesInfo(Messages.getMessage("info_message.diff_cancel"))) {
            throw new SVNPluginException();
        }
        kitUtils.setSVNUtils(utils);
    }

    public void displayDiff(String pjPath, String workFile) throws SVNPluginException {
        // 引数チェック
        if (pjPath == null || workFile == null || pjPath.length() == 0 || workFile.length() == 0) {
            throw new SVNPluginException(Messages.getMessage("err_message.common_file_not_found"));
        }

        // プログレスバー設定
        final SVNProgressDialog diffDialog = new SVNProgressDialog(
                (utils.getViewManager()).getMainFrame(),
                Messages.getMessage("progress_diff_title"),
                Messages.getMessage("progress_diff_message"));

        // 差分表示
        SVNDiffSwingWorker diffTask = new SVNDiffSwingWorker(pjPath, workFile, true);
        diffTask.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if ("progress".equals(evt.getPropertyName()) && (Integer) evt.getNewValue() == 100) {
                    diffDialog.dispose();
                }
            }
        });

        diffTask.execute();
        diffDialog.setVisible(true);
    }
}
