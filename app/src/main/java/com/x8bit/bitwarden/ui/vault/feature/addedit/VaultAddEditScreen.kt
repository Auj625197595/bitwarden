package com.x8bit.bitwarden.ui.vault.feature.addedit

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.components.BasicDialogState
import com.x8bit.bitwarden.ui.platform.components.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.BitwardenErrorContent
import com.x8bit.bitwarden.ui.platform.components.BitwardenLoadingContent
import com.x8bit.bitwarden.ui.platform.components.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.BitwardenOverflowActionItem
import com.x8bit.bitwarden.ui.platform.components.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextButton
import com.x8bit.bitwarden.ui.platform.components.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.LoadingDialogState
import com.x8bit.bitwarden.ui.platform.components.OverflowMenuItemData
import com.x8bit.bitwarden.ui.platform.manager.permissions.PermissionsManager
import com.x8bit.bitwarden.ui.platform.theme.LocalPermissionsManager
import com.x8bit.bitwarden.ui.platform.util.persistentListOfNotNull
import com.x8bit.bitwarden.ui.tools.feature.generator.model.GeneratorMode
import com.x8bit.bitwarden.ui.vault.feature.addedit.handlers.VaultAddEditCardTypeHandlers
import com.x8bit.bitwarden.ui.vault.feature.addedit.handlers.VaultAddEditCommonHandlers
import com.x8bit.bitwarden.ui.vault.feature.addedit.handlers.VaultAddEditIdentityTypeHandlers
import com.x8bit.bitwarden.ui.vault.feature.addedit.handlers.VaultAddEditLoginTypeHandlers

/**
 * Top level composable for the vault add item screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun VaultAddEditScreen(
    onNavigateBack: () -> Unit,
    onNavigateToQrCodeScanScreen: () -> Unit,
    viewModel: VaultAddEditViewModel = hiltViewModel(),
    permissionsManager: PermissionsManager = LocalPermissionsManager.current,
    onNavigateToManualCodeEntryScreen: () -> Unit,
    onNavigateToGeneratorModal: (GeneratorMode.Modal) -> Unit,
    onNavigateToAttachments: (cipherId: String) -> Unit,
    onNavigateToMoveToOrganization: (cipherId: String) -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = context.resources

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is VaultAddEditEvent.NavigateToQrCodeScan -> {
                onNavigateToQrCodeScanScreen()
            }

            is VaultAddEditEvent.NavigateToManualCodeEntry -> {
                onNavigateToManualCodeEntryScreen()
            }

            is VaultAddEditEvent.NavigateToGeneratorModal -> {
                onNavigateToGeneratorModal(event.generatorMode)
            }

            is VaultAddEditEvent.ShowToast -> {
                Toast.makeText(context, event.message(resources), Toast.LENGTH_SHORT).show()
            }

            is VaultAddEditEvent.NavigateToAttachments -> onNavigateToAttachments(event.cipherId)
            is VaultAddEditEvent.NavigateToMoveToOrganization -> {
                onNavigateToMoveToOrganization(event.cipherId)
            }

            is VaultAddEditEvent.NavigateToCollections -> {
                // TODO implement Collections in BIT-1575
                Toast.makeText(context, "Not yet implemented.", Toast.LENGTH_SHORT).show()
            }

            VaultAddEditEvent.NavigateBack -> onNavigateBack.invoke()
        }
    }

    val loginItemTypeHandlers = remember(viewModel) {
        VaultAddEditLoginTypeHandlers.create(viewModel = viewModel)
    }

    val commonTypeHandlers = remember(viewModel) {
        VaultAddEditCommonHandlers.create(viewModel = viewModel)
    }

    val identityItemTypeHandlers = remember(viewModel) {
        VaultAddEditIdentityTypeHandlers.create(viewModel = viewModel)
    }

    val cardItemTypeHandlers = remember(viewModel) {
        VaultAddEditCardTypeHandlers.create(viewModel = viewModel)
    }

    VaultAddEditItemDialogs(
        dialogState = state.dialog,
        onDismissRequest = remember(viewModel) {
            { viewModel.trySendAction(VaultAddEditAction.Common.DismissDialog) }
        },
    )

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = state.screenDisplayName(),
                navigationIcon = painterResource(id = R.drawable.ic_close),
                navigationIconContentDescription = stringResource(id = R.string.close),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(VaultAddEditAction.Common.CloseClick) }
                },
                scrollBehavior = scrollBehavior,
                actions = {
                    BitwardenTextButton(
                        label = stringResource(id = R.string.save),
                        onClick = remember(viewModel) {
                            { viewModel.trySendAction(VaultAddEditAction.Common.SaveClick) }
                        },
                    )
                    BitwardenOverflowActionItem(
                        menuItemDataList = persistentListOfNotNull(
                            OverflowMenuItemData(
                                text = stringResource(id = R.string.attachments),
                                onClick = remember(viewModel) {
                                    {
                                        viewModel.trySendAction(
                                            VaultAddEditAction.Common.AttachmentsClick,
                                        )
                                    }
                                },
                            )
                                .takeUnless { state.isAddItemMode },
                            OverflowMenuItemData(
                                text = stringResource(id = R.string.move_to_organization),
                                onClick = remember(viewModel) {
                                    {
                                        viewModel.trySendAction(
                                            VaultAddEditAction.Common.MoveToOrganizationClick,
                                        )
                                    }
                                },
                            )
                                .takeUnless { state.isAddItemMode || state.isCipherInCollection },
                            OverflowMenuItemData(
                                text = stringResource(id = R.string.collections),
                                onClick = remember(viewModel) {
                                    {
                                        viewModel.trySendAction(
                                            VaultAddEditAction.Common.CollectionsClick,
                                        )
                                    }
                                },
                            )
                                .takeUnless { state.isAddItemMode || !state.isCipherInCollection },
                        ),
                    )
                },
            )
        },
    ) { innerPadding ->
        when (val viewState = state.viewState) {
            is VaultAddEditState.ViewState.Content -> {
                VaultAddEditContent(
                    state = viewState,
                    isAddItemMode = state.isAddItemMode,
                    onTypeOptionClicked = remember(viewModel) {
                        { viewModel.trySendAction(VaultAddEditAction.Common.TypeOptionSelect(it)) }
                    },
                    loginItemTypeHandlers = loginItemTypeHandlers,
                    commonTypeHandlers = commonTypeHandlers,
                    permissionsManager = permissionsManager,
                    identityItemTypeHandlers = identityItemTypeHandlers,
                    cardItemTypeHandlers = cardItemTypeHandlers,
                    modifier = Modifier
                        .imePadding()
                        .padding(innerPadding)
                        .fillMaxSize(),
                )
            }

            is VaultAddEditState.ViewState.Error -> {
                BitwardenErrorContent(
                    message = viewState.message(),
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                )
            }

            VaultAddEditState.ViewState.Loading -> {
                BitwardenLoadingContent(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun VaultAddEditItemDialogs(
    dialogState: VaultAddEditState.DialogState?,
    onDismissRequest: () -> Unit,
) {
    when (dialogState) {
        is VaultAddEditState.DialogState.Loading -> {
            BitwardenLoadingDialog(
                visibilityState = LoadingDialogState.Shown(dialogState.label),
            )
        }

        is VaultAddEditState.DialogState.Generic -> {
            BitwardenBasicDialog(
                visibilityState = BasicDialogState.Shown(
                    title = dialogState.title,
                    message = dialogState.message,
                ),
                onDismissRequest = onDismissRequest,
            )
        }

        null -> Unit
    }
}
