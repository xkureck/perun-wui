package cz.metacentrum.perun.wui.registrar.pages.steps;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HTML;
import cz.metacentrum.perun.wui.client.resources.PerunConfiguration;
import cz.metacentrum.perun.wui.client.utils.JsUtils;
import cz.metacentrum.perun.wui.client.utils.Utils;
import cz.metacentrum.perun.wui.json.ErrorTranslator;
import cz.metacentrum.perun.wui.json.Events;
import cz.metacentrum.perun.wui.json.JsonEvents;
import cz.metacentrum.perun.wui.json.managers.AttributesManager;
import cz.metacentrum.perun.wui.json.managers.RegistrarManager;
import cz.metacentrum.perun.wui.model.GeneralObject;
import cz.metacentrum.perun.wui.model.PerunException;
import cz.metacentrum.perun.wui.model.beans.Application;
import cz.metacentrum.perun.wui.model.beans.Attribute;
import cz.metacentrum.perun.wui.model.beans.Group;
import cz.metacentrum.perun.wui.model.beans.Vo;
import cz.metacentrum.perun.wui.model.common.PerunPrincipal;
import cz.metacentrum.perun.wui.registrar.client.resources.PerunRegistrarTranslation;
import cz.metacentrum.perun.wui.registrar.pages.FormView;
import cz.metacentrum.perun.wui.widgets.AlertErrorReporter;
import cz.metacentrum.perun.wui.widgets.PerunButton;
import cz.metacentrum.perun.wui.widgets.resources.PerunButtonType;
import org.gwtbootstrap3.client.ui.Alert;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Column;
import org.gwtbootstrap3.client.ui.Heading;
import org.gwtbootstrap3.client.ui.Icon;
import org.gwtbootstrap3.client.ui.ListGroup;
import org.gwtbootstrap3.client.ui.ListGroupItem;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.ModalFooter;
import org.gwtbootstrap3.client.ui.constants.AlertType;
import org.gwtbootstrap3.client.ui.constants.ButtonSize;
import org.gwtbootstrap3.client.ui.constants.ButtonType;
import org.gwtbootstrap3.client.ui.constants.ColumnOffset;
import org.gwtbootstrap3.client.ui.constants.ColumnSize;
import org.gwtbootstrap3.client.ui.constants.HeadingSize;
import org.gwtbootstrap3.client.ui.constants.IconPosition;
import org.gwtbootstrap3.client.ui.constants.IconSize;
import org.gwtbootstrap3.client.ui.constants.IconType;
import org.gwtbootstrap3.client.ui.constants.ListGroupItemType;
import org.gwtbootstrap3.client.ui.constants.ModalBackdrop;
import org.gwtbootstrap3.client.ui.constants.Pull;
import org.gwtbootstrap3.client.ui.html.Paragraph;
import org.gwtbootstrap3.client.ui.html.Text;
import org.gwtbootstrap3.extras.notify.client.constants.NotifyType;
import org.gwtbootstrap3.extras.notify.client.ui.Notify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Represents a final step in registration process. Show info.
 * Contains methods caseXxx(...) e.g. caseVoInitGroupInit is called in usecase [ VO initial application -> Group application ] etc.
 *
 * @author Ondrej Velisek <ondrejvelisek@gmail.com>
 */
public class SummaryStep implements Step {

	private FormView formView;
	private PerunRegistrarTranslation translation;

	private final String TARGET_EXISTING = "targetexisting";
	private final String TARGET_NEW = "targetnew";
	private final String TARGET_EXTENDED = "targetextended";

	private boolean exceptionDisplayed = false;
	private boolean modalDisplayed = false;
	private String redirectTo = null;

	public SummaryStep(FormView formView) {
		this.formView = formView;
		this.translation = formView.getTranslation();
	}

	@Override
	public void call(final PerunPrincipal pp, Summary summary, Events<Result> events) {

		Heading title = new Heading(HeadingSize.H2);
		ListGroup messages = new ListGroup();

		if (summary.containsGroupInitResult()) {
			if (summary.containsVoInitResult()) {
				caseVoInitGroupInit(summary, title, messages);
			} else if (summary.containsVoExtResult()) {
				caseVoExtGroupInit(summary, title, messages);
			} else {
				caseGroupInit(summary, title, messages);
			}
		} else if (summary.containsGroupExtResult()) {
			if (summary.containsVoExtResult()) {
				caseVoExtGroupExt(summary, title, messages);
			} else {
				caseGroupExt(summary, title, messages);
			}
		} else {
			if (summary.containsVoInitResult()) {
				caseVoInit(summary, title, messages);
			} else if (summary.containsVoExtResult()) {
				caseVoExt(summary, title, messages);
			} else {
				// Steps should not be empty.
			}
		}

		/**
		 * FIXME - Temporary change forcing all extending Metacentrum users to change their password due to need to re-sign all keys in kerberos database
		 */
		if (summary.containsVoExtResult()) {

			if (summary.getVoExtResult().isOk()) {

				AttributesManager.getUserAttributes(pp.getUserId(), Arrays.asList("urn:perun:user:attribute-def:def:login-namespace:einfra",
						"urn:perun:user:attribute-def:def:changedPassMeta"), new JsonEvents() {
					@Override
					public void onFinished(JavaScriptObject result) {

						ArrayList<Attribute> list = JsUtils.jsoAsList(result);
						boolean hasEinfraLogin = false;
						if (list != null) {
							for (Attribute a : list) {
								if (Objects.equals("login-namespace:einfra", a.getFriendlyName())) {
									if (a.getValue() != null) {
										hasEinfraLogin = true;
										break;
									}
								}
							}

							if (hasEinfraLogin) {
								for (Attribute a : list) {
									if (Objects.equals("changedPassMeta", a.getFriendlyName())) {
										if (a.getValue() == null) {
											displayMetaCentrumWarning();
										}
									}
								}
							}
						}
					}

					@Override
					public void onError(PerunException error) {
						// display anyway
						if (((Vo)summary.getVoExtResult().getBean()).getShortName().equals("meta") ||
						((Vo)summary.getVoExtResult().getBean()).getShortName().equals("einfra") ||
						((Vo)summary.getVoExtResult().getBean()).getShortName().equals("storage")) {
							displayMetaCentrumWarning();
						}
					}

					@Override
					public void onLoadingStart() {

					}
				});

			}

		}


	}

	private void displayMetaCentrumWarning() {

		final Modal modal = new Modal();
		modal.setTitle(translation.metaResetHeading());
		modal.setFade(true);
		modal.setDataKeyboard(false);
		modal.setDataBackdrop(ModalBackdrop.STATIC);
		modal.setClosable(false);

		ModalBody body = new ModalBody();
		body.add(new HTML(translation.metaResetText()));

		ModalFooter footer = new ModalFooter();

		final Button reset = new Button(translation.metaResetButton(), new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				modal.hide();
				Window.Location.assign(Utils.getPasswordResetLink("einfra"));
			}
		});
		reset.setType(ButtonType.SUCCESS);
		reset.setIcon(IconType.CHEVRON_RIGHT);
		reset.setIconPosition(IconPosition.RIGHT);
		reset.setIconFixedWidth(true);

		final Button no = new Button(translation.offerMembershipExtensionNoThanks(), new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				modal.hide();
			}
		});
		no.setType(ButtonType.DANGER);

		footer.add(no);
		footer.add(reset);

		modal.add(body);
		modal.add(footer);

		Timer timer = new Timer() {
			@Override
			public void run() {
				modal.show();
			}
		};
		timer.schedule(500);

	}

	/**
	 * Fill summary with result about VO initial application
	 *
	 * @param summary
	 * @param title
	 * @param messages
	 */
	private void caseVoInit(Summary summary, Heading title, ListGroup messages) {
		Result res = summary.getVoInitResult();
		if (res.isOk() && summary.mustRevalidateEmail() == null) {
			title.add(successIcon());
			ListGroupItem msg = new ListGroupItem();

			if (res.hasAutoApproval()) {
				title.add(new Text(" "+translation.initTitleAutoApproval()));
				msg.setText(translation.registered(res.getBean().getName()));
			} else {
				title.add(new Text(" "+translation.initTitle()));
				msg.setText(translation.waitForAcceptation());
			}

			messages.add(msg);
		} else if (res.isOk() && summary.mustRevalidateEmail() != null) {
			title.add(new Icon(IconType.WARNING));
			title.add(new Text(" " + translation.emailVerificationNeededTitle()));

			verifyMailMessage(summary, messages, summary.getApplication().getId());
		} else if (res.getException() != null && "CantBeApprovedException".equals(res.getException().getName())) {

			// FIXME - hack to ignore CantBeApprovedException since VO manager can manually handle it.
			title.add(successIcon());
			ListGroupItem msg = new ListGroupItem();
			title.add(new Text(" "+translation.initTitle()));
			msg.setText(translation.waitForAcceptation());
			messages.add(msg);
			verifyMailMessage(summary, messages, summary.getApplication().getId());

		} else {
			displayException(res.getException(), res.getBean());
		}

		// Show continue button
		PerunButton continueBtn = null;
		if (summary.alreadyMemberOfVo()) {
			// user visited registration form, but is already registered
			continueBtn = getContinueButton(TARGET_EXISTING);
		} else if (summary.alreadyAppliedToVo()) {
			// user visited registration form again (back from service?), his registration is not yet verified/approved
			continueBtn = getContinueButton(TARGET_NEW, res);
		} else if (res.isOk() || res.getException().getName().equals("RegistrarException")) {
			// user submitted registration, it might have failed
			continueBtn = getContinueButton(TARGET_NEW, res);
		}

		displaySummary(title, messages, continueBtn);
	}


	/**
	 * Fill summary with result about VO extension application
	 *
	 * @param summary
	 * @param title
	 * @param messages
	 */
	private void caseVoExt(Summary summary, Heading title, ListGroup messages) {
		Result res = summary.getVoExtResult();
		if (res.isOk()) {
			title.add(successIcon());
			ListGroupItem msg = new ListGroupItem();

			if (res.hasAutoApproval()) {
				title.add(new Text(" "+translation.extendTitleAutoApproval()));
				msg.setText(translation.extended(res.getBean().getName()));
			} else {
				title.add(new Text(" "+translation.extendTitle()));
				msg.setText(translation.waitForExtAcceptation());
			}

			messages.add(msg);
			verifyMailMessage(summary, messages, summary.getApplication().getId());

		} else if (res.getException() != null && "CantBeApprovedException".equals(res.getException().getName())) {

			// FIXME - hack to ignore CantBeApprovedException since VO manager can manually handle it.
			title.add(successIcon());
			ListGroupItem msg = new ListGroupItem();
			title.add(new Text(" "+translation.extendTitle()));
			msg.setText(translation.waitForExtAcceptation());
			messages.add(msg);
			verifyMailMessage(summary, messages, summary.getApplication().getId());

		} else {
			// FIXME - solve this BLEEEH hack in better way.
			if (res.getException() != null && "DuplicateRegistrationAttemptException".equals(res.getException().getName())) {
				res.getException().setName("DuplicateExtensionAttemptException");
			}
			displayException(res.getException(), res.getBean());
		}

		// Show continue button
		PerunButton continueBtn = null;
		if (res.isOk() || res.getException().getName().equals("RegistrarException")) {
			continueBtn = getContinueButton(TARGET_EXTENDED, res);
		} else if (summary.alreadyAppliedForVoExtension()) {
			continueBtn = getContinueButton(TARGET_EXTENDED, res);
		} else if (summary.alreadyMemberOfVo()) {
			continueBtn = getContinueButton(TARGET_EXISTING);
		}

		// FIXME: HACK for ELIXIR - if is member and link should go out of registrar, leave immediatelly
		if (summary.alreadyMemberOfVo() && !summary.alreadyAppliedForVoExtension()) {
			// only when there is no pending extension application !!
			String url = Window.Location.getParameter(TARGET_EXISTING);
			if (url != null && !url.isEmpty()) {
				Window.Location.assign(url);
			}
		}

		// for others display summary
		displaySummary(title, messages, continueBtn);

	}

	/**
	 * Fill summary with result about Group initial application
	 *
	 * @param summary
	 * @param title
	 * @param messages
	 */
	private void caseGroupInit(Summary summary, Heading title, ListGroup messages) {
		Result res = summary.getGroupInitResult();
		if (res.isOk()) {
			title.add(successIcon());
			ListGroupItem msg = new ListGroupItem();

			if (res.hasAutoApproval()) {
				if (summary.alreadyAppliedToVo()) {
					title.add(new Text(" "+translation.initTitle()));
					msg.setText(translation.waitForVoAcceptation(((Group) res.getBean()).getShortName()));
				} else {
					title.add(new Text(" "+translation.initTitleAutoApproval()));
					msg.setText(translation.registered(((Group) res.getBean()).getShortName()));
				}
			} else {
				title.add(new Text(" "+translation.initTitle()));
				msg.setText(translation.waitForAcceptation());
			}

			messages.add(msg);
			verifyMailMessage(summary, messages, summary.getApplication().getId());

		} else if (res.getException() != null && "CantBeApprovedException".equals(res.getException().getName())) {

			// FIXME - hack to ignore CantBeApprovedException since VO manager can manually handle it.
			title.add(successIcon());
			ListGroupItem msg = new ListGroupItem();

			title.add(new Text(" "+translation.initTitle()));
			msg.setText(translation.waitForAcceptation());

			messages.add(msg);
			verifyMailMessage(summary, messages, summary.getApplication().getId());

		} else {
			displayException(res.getException(), res.getBean());
		}

		// Show continue button
		PerunButton continueBtn = null;
		if (summary.alreadyMemberOfGroup()) {
			continueBtn = getContinueButton(TARGET_EXISTING);
		} else if (summary.alreadyAppliedToGroup()) {
			continueBtn = getContinueButton(TARGET_NEW, res);
		} else if (res.isOk() || res.getException().getName().equals("RegistrarException")) {
			continueBtn = getContinueButton(TARGET_NEW, res);
		}

		displaySummary(title, messages, continueBtn);
	}

	/**
	 * Fill summary with result about Group extension application
	 *
	 * @param summary
	 * @param title
	 * @param messages
	 */
	private void caseGroupExt(Summary summary, Heading title, ListGroup messages) {
		Result res = summary.getGroupExtResult();
		if (res.isOk()) {
			title.add(successIcon());
			ListGroupItem msg = new ListGroupItem();

			if (res.hasAutoApproval()) {
				if (summary.alreadyAppliedForVoExtension()) {
					title.add(new Text(" " + translation.extendTitle()));
					msg.setText(translation.waitForVoExtension(((Group) res.getBean()).getShortName()));
				} else {
					title.add(new Text(" " + translation.extendTitleAutoApproval()));
					msg.setText(translation.extended(((Group) res.getBean()).getShortName()));
				}
			} else {
				title.add(new Text(" "+translation.extendTitle()));
				msg.setText(translation.waitForExtAcceptation());
			}

			messages.add(msg);
			verifyMailMessage(summary, messages, summary.getApplication().getId());

		} else if (res.getException() != null && "CantBeApprovedException".equals(res.getException().getName())) {

			// FIXME - hack to ignore CantBeApprovedException since VO manager can manually handle it.
			title.add(successIcon());
			ListGroupItem msg = new ListGroupItem();

			title.add(new Text(" "+translation.extendTitle()));
			msg.setText(translation.waitForExtAcceptation());

			messages.add(msg);
			verifyMailMessage(summary, messages, summary.getApplication().getId());

		} else {
			// FIXME - solve this BLEEEH hack in better way.
			if (res.getException() != null && "DuplicateRegistrationAttemptException".equals(res.getException().getName())) {
				res.getException().setName("DuplicateExtensionAttemptException");
			}
			displayException(res.getException(), res.getBean());
		}

		// Show continue button
		PerunButton continueBtn = null;
		if (summary.alreadyMemberOfGroup()) {
			continueBtn = getContinueButton(TARGET_EXISTING);
		} else if (summary.alreadyAppliedForGroupExtension()) {
			continueBtn = getContinueButton(TARGET_EXTENDED, res);
		} else if (res.isOk() || res.getException().getName().equals("RegistrarException")) {
			continueBtn = getContinueButton(TARGET_EXTENDED, res);
		}

		displaySummary(title, messages, continueBtn);
	}

	/**
	 * Fill summary with result about VO initial and Group initial application
	 *
	 * @param summary
	 * @param title
	 * @param messages
	 */
	private void caseVoInitGroupInit(Summary summary, Heading title, ListGroup messages) {

		Result resultVo = summary.getVoInitResult();
		Result resultGroup = summary.getGroupInitResult();

		// Show summary about initial application to VO
		if (resultVo.isOk()) {
			ListGroupItem msg = new ListGroupItem();

			if (resultVo.hasAutoApproval()) {
				msg.setText(translation.registered(resultVo.getBean().getName()));
			} else {
				// Message from group application is sufficient in this case.
			}

			if (!msg.getText().isEmpty()) {
				messages.add(msg);
			}
		} else if (resultVo.getException() != null && "CantBeApprovedException".equals(resultVo.getException().getName())) {

			// FIXME - hack to ignore CantBeApprovedException since VO manager can manually handle it.
			ListGroupItem msg = new ListGroupItem();
			msg.setText(translation.waitForAcceptation());
			messages.add(msg);

		} else {
			displayException(resultVo.getException(), resultVo.getBean());
		}

		verifyMailMessage(summary, messages, summary.getApplication().getId());

		// Show summary about application to group
		if (resultGroup.isOk()) {

			title.add(successIcon());
			ListGroupItem msg = new ListGroupItem();

			if (resultGroup.hasAutoApproval()) {
				if (resultVo.hasAutoApproval()) {
					title.add(new Text(" "+translation.initTitleAutoApproval()));
					msg.setText(translation.registered(((Group) resultGroup.getBean()).getShortName()));
				} else {
					title.add(new Text(" "+translation.initTitle()));
					msg.setText(translation.waitForVoAcceptation(((Group) resultGroup.getBean()).getShortName()));
				}
			} else {
				title.add(new Text(" "+translation.initTitle()));
				msg.setText(translation.waitForAcceptation());
			}

			messages.add(msg);

		} else if (resultGroup.getException() != null && "CantBeApprovedException".equals(resultGroup.getException().getName())) {

			// FIXME - hack to ignore CantBeApprovedException since VO/Group manager can manually handle it.
			title.add(successIcon());
			ListGroupItem msg = new ListGroupItem();

			title.add(new Text(" "+translation.initTitle()));
			msg.setText(translation.waitForAcceptation());

			messages.add(msg);

		} else {
			displayException(resultGroup.getException(), resultGroup.getBean());
		}

		// Show continue button
		PerunButton continueBtn = null;
		if (summary.alreadyMemberOfGroup()) {
			continueBtn = getContinueButton(TARGET_EXISTING);
		} else if (summary.alreadyAppliedToGroup()) {
			// FIXME - we must pass both results
			// NOTE - how is it handled, when both VO and group are awaiting approval ??
			continueBtn = getContinueButton(TARGET_NEW, resultGroup);
		} else if ((resultGroup.isOk() || resultGroup.getException().getName().equals("RegistrarException"))
				&& (resultVo.isOk() || resultVo.getException().getName().equals("RegistrarException"))) {
			// FIXME - we must pass both results
			continueBtn = getContinueButton(TARGET_NEW);
		}

		displaySummary(title, messages, continueBtn);
	}

	/**
	 * Fill summary with result about VO extension and Group initial application
	 *
	 * @param summary
	 * @param title
	 * @param messages
	 */
	private void caseVoExtGroupInit(Summary summary, Heading title, ListGroup messages) {

		Result resultVo = summary.getVoExtResult();
		Result resultGroup = summary.getGroupInitResult();

		// Show summary about extension application to VO
		if (resultVo.isOk()) {
			ListGroupItem msg = new ListGroupItem();

			if (resultVo.hasAutoApproval()) {
				msg.setText(translation.extended(resultVo.getBean().getName()));
			} else {
				// Message from group application is sufficient in this case.
			}

			if (!msg.getText().isEmpty()) {
				messages.add(msg);
			}

		} else if (resultVo.getException() != null && "CantBeApprovedException".equals(resultVo.getException().getName())) {

			// FIXME - hack to ignore CantBeApprovedException since VO manager can manually handle it.
			ListGroupItem msg = new ListGroupItem();
			msg.setText(translation.waitForExtAcceptation());
			messages.add(msg);

		} else {
			// FIXME - solve this BLEEEH hack in better way.
			if (resultVo.getException() != null && "DuplicateRegistrationAttemptException".equals(resultVo.getException().getName())) {
				resultVo.getException().setName("DuplicateExtensionAttemptException");
			}
			displayException(resultVo.getException(), resultVo.getBean());
		}

		// Show summary about application to group
		if (resultGroup.isOk()) {
			title.add(successIcon());
			ListGroupItem msg = new ListGroupItem();

			if (resultGroup.hasAutoApproval()) {
				if (resultVo.hasAutoApproval()) { // FIXME - tohle by se mělo vyhodnotit z předchozího stavu (není auto nebo byla chyba)
					title.add(new Text(" "+translation.initTitleAutoApproval()));
					msg.setText(translation.registered(((Group) resultGroup.getBean()).getShortName()));
				} else {
					title.add(new Text(" "+translation.initTitle()));
					msg.setText(translation.waitForVoExtension(((Group) resultGroup.getBean()).getShortName()));
				}
			} else {
				title.add(new Text(" "+translation.initTitle()));
				msg.setText(translation.waitForAcceptation());
			}

			messages.add(msg);

		} else if (resultGroup.getException() != null && "CantBeApprovedException".equals(resultGroup.getException().getName())) {

			// FIXME - hack to ignore CantBeApprovedException since VO/Group manager can manually handle it.
			title.add(successIcon());
			ListGroupItem msg = new ListGroupItem();

			title.add(new Text(" "+translation.initTitle()));
			msg.setText(translation.waitForAcceptation());

			messages.add(msg);

		} else {
			displayException(resultGroup.getException(), resultGroup.getBean());
		}

		// Show continue button
		PerunButton continueBtn = null;
		if (summary.alreadyMemberOfGroup()) {
			continueBtn = getContinueButton(TARGET_EXISTING);
		} else if (summary.alreadyAppliedToGroup()) {
			continueBtn = getContinueButton(TARGET_NEW, resultGroup);
		} else if (resultGroup.isOk() || resultGroup.getException().getName().equals("RegistrarException")) {
			continueBtn = getContinueButton(TARGET_NEW, resultGroup);
		}

		displaySummary(title, messages, continueBtn);
	}

	/**
	 * Fill summary with result about VO extension and Group extension application
	 *
	 * @param summary
	 * @param title
	 * @param messages
	 */
	private void caseVoExtGroupExt(Summary summary, Heading title, ListGroup messages) {

		Result resultVo = summary.getVoExtResult();
		Result resultGroup = summary.getGroupExtResult();

		// Show summary about extension application to VO
		if (resultVo.isOk()) {
			ListGroupItem msg = new ListGroupItem();

			if (resultVo.hasAutoApproval()) {
				msg.setText(translation.extended(resultVo.getBean().getName()));
			} else {
				// Message from group application is sufficient in this case.
			}

			if (!msg.getText().isEmpty()) {
				messages.add(msg);
			}

		} else if (resultVo.getException() != null && "CantBeApprovedException".equals(resultVo.getException().getName())) {

			// FIXME - hack to ignore CantBeApprovedException since VO manager can manually handle it.
			ListGroupItem msg = new ListGroupItem();
			msg.setText(translation.waitForExtAcceptation());
			messages.add(msg);

		} else {
			// FIXME - solve this BLEEEH hack in better way.
			if (resultVo.getException() != null && "DuplicateRegistrationAttemptException".equals(resultVo.getException().getName())) {
				resultVo.getException().setName("DuplicateExtensionAttemptException");
			}
			displayException(resultVo.getException(), resultVo.getBean());
		}

		// Show summary about application to group
		if (resultGroup.isOk()) {
			title.add(successIcon());
			ListGroupItem msg = new ListGroupItem();

			if (resultGroup.hasAutoApproval()) {
				if (resultVo.hasAutoApproval()) {
					title.add(new Text(" "+translation.extendTitleAutoApproval()));
					msg.setText(translation.extended(((Group) resultGroup.getBean()).getShortName()));
				} else {
					title.add(new Text(" "+translation.extendTitle()));
					msg.setText(translation.waitForVoExtension(((Group) resultGroup.getBean()).getShortName()));
				}
			} else {
				title.add(new Text(" "+translation.extendTitle()));
				msg.setText(translation.waitForExtAcceptation());
			}

			messages.add(msg);

		} else if (resultGroup.getException() != null && "CantBeApprovedException".equals(resultGroup.getException().getName())) {

			// FIXME - hack to ignore CantBeApprovedException since VO/Group manager can manually handle it.
			title.add(successIcon());
			ListGroupItem msg = new ListGroupItem();

			title.add(new Text(" "+translation.extendTitle()));
			msg.setText(translation.waitForExtAcceptation());

			messages.add(msg);

		} else {
			// FIXME - solve this BLEEEH hack in better way.
			if ("DuplicateRegistrationAttemptException".equals(resultVo.getException().getName())) {
				resultVo.getException().setName("DuplicateExtensionAttemptException");
			}
			displayException(resultGroup.getException(), resultGroup.getBean());
		}

		// Show continue button
		PerunButton continueBtn = null;
		if (summary.alreadyMemberOfGroup()) {
			continueBtn = getContinueButton(TARGET_EXISTING);
		} else if (summary.alreadyAppliedForGroupExtension()) {
			continueBtn = getContinueButton(TARGET_EXTENDED, resultGroup);
		} else if (resultGroup.isOk() || resultGroup.getException().getName().equals("RegistrarException")) {
			continueBtn = getContinueButton(TARGET_EXTENDED, resultGroup);
		}

		displaySummary(title, messages, continueBtn);
	}

	private Icon successIcon() {
		Icon success = new Icon(IconType.CHECK_CIRCLE);
		success.setColor("#5cb85c");
		return success;
	}

	private void verifyMailMessage(Summary summary, ListGroup messages, int appId) {
		if (summary.mustRevalidateEmail() != null) {
			ListGroupItem verifyMail = new ListGroupItem();
			verifyMail.add(new Paragraph(" " + translation.verifyMail(summary.mustRevalidateEmail())));
			verifyMail.setType(ListGroupItemType.WARNING);
			AlertErrorReporter errorReporter = new AlertErrorReporter();
			errorReporter.setVisible(false);
			errorReporter.setType(AlertType.WARNING);
			errorReporter.setMarginTop(20);

			PerunButton resendButton = new PerunButton();
			resendButton.setType(ButtonType.WARNING);
			resendButton.setText(translation.reSendMailVerificationButton());
			resendButton.addClickHandler(event ->
				RegistrarManager.sendMessage(appId, "MAIL_VALIDATION", null, new JsonEvents() {
				@Override
				public void onFinished(JavaScriptObject result) {
					resendButton.setProcessing(false);
					Notify.notify(translation.mailVerificationRequestSent(summary.mustRevalidateEmail()), NotifyType.SUCCESS);
				}

				@Override
				public void onError(PerunException error) {
					errorReporter.setVisible(true);
					errorReporter.setHTML(ErrorTranslator.getTranslatedMessage(error));
					errorReporter.setReportInfo(error);
					resendButton.setProcessing(false);
				}

				@Override
				public void onLoadingStart() {
					errorReporter.setVisible(false);
					resendButton.setProcessing(true);
				}
			}));
			verifyMail.add(resendButton);
			verifyMail.add(errorReporter);
			messages.add(verifyMail);
		}
	}

	private void displaySummary(Heading title, ListGroup messages, PerunButton continueButton) {

		boolean skipSummary = false;

		Vo resultVo = null;
		Group resultGroup = null;
		if (formView != null && formView.getForm() != null && formView.getForm().getApp() != null) {
			resultVo = formView.getForm().getApp().getVo();
			resultGroup = formView.getForm().getApp().getGroup();
		}

		String resultKey = (resultVo != null) ? resultVo.getShortName() : "";
		if (resultGroup != null) {
			resultKey += ":" + resultGroup.getName();
		}

		List<String> skippedSummary = PerunConfiguration.getRegistrarSkipSummaryFor();
		skipSummary = skippedSummary.contains(resultKey);

		if (skipSummary && !exceptionDisplayed && continueButton != null && redirectTo != null) {

			// make sure LDAP backend is updated and go to the end-service
			redirectAfterTimeout(redirectTo);

		} else {

			// show summary
			if (title != null || title.getWidgetCount() != 0 || !title.getText().isEmpty()) {
				formView.getForm().add(title);
			}
			if (messages != null || messages.getWidgetCount() != 0) {
				formView.getForm().add(messages);
			}
			if (continueButton != null) {
				formView.getForm().add(continueButton);
			}

		}

	}

	private PerunButton getContinueButton(final String urlParameter) {
		return getContinueButton(urlParameter, null);
	}

	private PerunButton getContinueButton(final String urlParameter, final Result previousResult) {

		if (Window.Location.getParameter(urlParameter) != null) {

			redirectTo = Window.Location.getParameter(urlParameter);

			PerunButton continueButton = PerunButton.getButton(PerunButtonType.CONTINUE);
			// make button more visible to the users
			continueButton.setSize(ButtonSize.LARGE);
			continueButton.setType(ButtonType.SUCCESS);

			final ClickHandler clickHandler = new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					redirectAfterTimeout(redirectTo);
				}
			};

			if (redirectTo != null && !redirectTo.isEmpty()) {

				int applicationId;
				if (previousResult.getApplication() != null) {
					// in case user has just submitted the application
					applicationId = previousResult.getApplication().getId();
				} else {
					// in case the user has tried again to submit the application
					applicationId = previousResult.getException().getApplication().getId();
				}

				final Modal modal = new Modal();
				modal.setFade(true);
				modal.setDataKeyboard(false);
				modal.setDataBackdrop(ModalBackdrop.STATIC);
				modal.setClosable(false);
				modal.setWidth("750px");


				Button button = new Button(translation.understand());
				button.setType(ButtonType.PRIMARY);
				button.addClickHandler(new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						modal.hide();
					}
				});

				Button button2 = new Button(translation.continueAnyway());
				button2.setType(ButtonType.DEFAULT);
				button2.addClickHandler(new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						// perform final redirect
						modal.hide();
						clickHandler.onClick(event);
					}
				});

				ModalFooter footer = new ModalFooter();
				footer.add(button);

				ModalBody body = new ModalBody();
				modal.add(body);
				modal.add(footer);

				continueButton.addClickHandler(new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {

						// check if
						RegistrarManager.getApplicationById(applicationId, new JsonEvents() {
							@Override
							public void onFinished(JavaScriptObject result) {
								Application application = result.cast();
								String text = (Application.ApplicationState.NEW.equals(application.getState())) ? translation.redirectWaitForVerification() : translation.redirectWaitForApproval();
								modal.setTitle(application.getTranslatedState());
								final Paragraph content =new Paragraph(text);
								body.clear();
								body.add(content);

								continueButton.setProcessing(false);
								Application currentAppState = result.cast();

								if (Application.ApplicationState.NEW.equals(currentAppState.getState())) {
									content.setHTML(translation.redirectWaitForVerification());
									// modal will allow redirect
									modal.show();
								} else if (Application.ApplicationState.VERIFIED.equals(currentAppState.getState()) &&
									previousResult.getException() != null) {
									// verified in the mean time
									PerunException fakeException = PerunException.createNew("0", "DuplicateRegistrationAttemptException", "");
									fakeException.setApplication(currentAppState);
									displayException(fakeException, (currentAppState.getGroup() != null) ? currentAppState.getGroup() : currentAppState.getVo());
									formView.hideMailVerificationAlert();
									content.setHTML(translation.redirectWaitForApproval());
									// modal will allow redirect
									modal.show();
								} else if (Application.ApplicationState.APPROVED.equals(currentAppState.getState())) {
									// no modal needed - click to go
									formView.hideNotice();
									clickHandler.onClick(event);
								}

							}

							@Override
							public void onError(PerunException error) {
								// FIXME - this BLEEEH hack
								// should display some info to the user, but I am not sure where exactly
								// in this context
							}

							@Override
							public void onLoadingStart() {
								continueButton.setProcessing(true);
							}
						});

					}
				});

			} else {
				continueButton.addClickHandler(clickHandler);
			}
			return continueButton;
		}
		return null;
	}

	/**
	 * Clears form and perform redirect after specified time. Also prints info about redirection.
	 *
	 * @param redirectTo URL to redirect to
	 */
	private void redirectAfterTimeout(String redirectTo) {

		formView.getForm().clear();
		formView.hideMailVerificationAlert();

		Heading head = new Heading(HeadingSize.H4, translation.redirectingBackToService());
		Icon spin = new Icon(IconType.SPINNER);
		spin.setSpin(true);
		spin.setSize(IconSize.LARGE);
		spin.setPull(Pull.LEFT);
		spin.setMarginTop(10);

		Column column = new Column(ColumnSize.MD_8, ColumnSize.LG_6, ColumnSize.SM_10, ColumnSize.XS_12);
		column.setOffset(ColumnOffset.MD_2,ColumnOffset.LG_3,ColumnOffset.SM_1,ColumnOffset.XS_0);

		column.add(spin);
		column.add(head);
		column.setMarginTop(30);

		formView.getForm().add(column);

		// WAIT 7 SEC BEFORE REDIRECT back to service so that LDAP in Perun is updated
		Timer timer = new Timer() {
			@Override
			public void run() {
				Window.Location.assign(redirectTo);
			}
		};
		timer.schedule(7000);
	}

	private void displayException(PerunException ex, GeneralObject bean) {
		formView.displayException(ex, bean);
		exceptionDisplayed = true;
	}

	@Override
	public Result getResult() {
		// Nobody should be after Summary step so nobody can call it.
		return null;
	}
}
