package it.niedermann.nextcloud.deck.ui.board;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.DialogFragment;

import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import it.niedermann.nextcloud.deck.Application;
import it.niedermann.nextcloud.deck.R;
import it.niedermann.nextcloud.deck.model.AccessControl;
import it.niedermann.nextcloud.deck.model.full.FullBoard;
import it.niedermann.nextcloud.deck.persistence.sync.SyncManager;

import static it.niedermann.nextcloud.deck.persistence.sync.adapters.db.util.LiveDataHelper.observeOnce;

public class ShareBoardDialogFragment extends DialogFragment {

    private static final String KEY_ACCOUNT_ID = "account_id";
    private static final String KEY_BOARD_ID = "board_id";
    private static final Long NO_BOARD_ID = -1L;

    @BindView(R.id.peopleList)
    LinearLayout peopleList;

    /**
     * Use newInstance()-Method
     */
    private ShareBoardDialogFragment() {
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Activity activity = Objects.requireNonNull(getActivity());
        View view = activity.getLayoutInflater().inflate(R.layout.dialog_board_share, null);
        ButterKnife.bind(this, view);
        Long boardId = Objects.requireNonNull(getArguments()).getLong(KEY_BOARD_ID);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity, Application.getAppTheme(getContext()) ? R.style.DialogDarkTheme : R.style.ThemeOverlay_AppCompat_Dialog_Alert);

        if (NO_BOARD_ID.equals(boardId)) {
            throw new IllegalArgumentException("boardId does not exist");
        } else {
            SyncManager syncManager = new SyncManager(activity);
            final long accountId = Objects.requireNonNull(getArguments()).getLong(KEY_ACCOUNT_ID);
            observeOnce(syncManager.getFullBoardById(accountId, boardId), ShareBoardDialogFragment.this, (FullBoard fb) -> {
                if (fb.board != null) {
                    for (AccessControl ac : fb.getParticipants()) {
                        View v = getLayoutInflater().inflate(R.layout.fragment_board_share_user, null);
                        if (ac.getUser() != null)
                            ((TextView) v.findViewById(R.id.username)).setText(ac.getUser().getUid());

                        SwitchCompat switchEdit = v.findViewById(R.id.permission_edit);
                        switchEdit.setChecked(ac.isPermissionEdit());
                        switchEdit.setOnCheckedChangeListener((buttonView, isChecked) -> {
                            ac.setPermissionEdit(isChecked);
                            syncManager.updateAccessControl(ac);
                        });

                        SwitchCompat switchManage = v.findViewById(R.id.permission_manage);
                        switchManage.setChecked(ac.isPermissionManage());
                        switchManage.setOnCheckedChangeListener((buttonView, isChecked) -> {
                            syncManager.updateAccessControl(ac);
                            ac.setPermissionManage(isChecked);
                        });

                        SwitchCompat switchShare = v.findViewById(R.id.permission_share);
                        switchShare.setChecked(ac.isPermissionShare());
                        switchShare.setOnCheckedChangeListener((buttonView, isChecked) -> {
                            ac.setPermissionShare(isChecked);
                            syncManager.updateAccessControl(ac);
                        });

                        peopleList.addView(v);
                    }
                }
            });
        }

        return dialogBuilder
                .setView(view)
                .setPositiveButton(R.string.simple_close, (dialog, which) -> {
                })
                .create();
    }

    public static ShareBoardDialogFragment newInstance(@NonNull Long accountId, @NonNull Long boardId) {
        ShareBoardDialogFragment dialog = new ShareBoardDialogFragment();

        Bundle args = new Bundle();
        args.putLong(KEY_ACCOUNT_ID, accountId);
        args.putLong(KEY_BOARD_ID, boardId);
        dialog.setArguments(args);

        return dialog;
    }

}