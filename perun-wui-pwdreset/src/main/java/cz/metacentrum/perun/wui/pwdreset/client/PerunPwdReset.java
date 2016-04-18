package cz.metacentrum.perun.wui.pwdreset.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.gwtplatform.mvp.client.RootPresenter;
import com.gwtplatform.mvp.client.annotations.DefaultPlace;
import com.gwtplatform.mvp.client.annotations.ErrorPlace;
import com.gwtplatform.mvp.client.annotations.UnauthorizedPlace;
import com.gwtplatform.mvp.client.gin.AbstractPresenterModule;
import com.gwtplatform.mvp.client.gin.DefaultModule;
import cz.metacentrum.perun.wui.client.PerunPlaceManager;
import cz.metacentrum.perun.wui.client.PerunRootPresenter;
import cz.metacentrum.perun.wui.client.resources.ExceptionLogger;
import cz.metacentrum.perun.wui.client.resources.PerunResources;
import cz.metacentrum.perun.wui.client.utils.Utils;
import cz.metacentrum.perun.wui.pages.*;
import cz.metacentrum.perun.wui.pwdreset.client.resources.PerunPwdResetResources;
import cz.metacentrum.perun.wui.pwdreset.client.resources.PerunPwdResetPlaceTokens;
import cz.metacentrum.perun.wui.pwdreset.pages.*;

/**
 * Entry point class and GWTP module for Perun WUI Password Reset.
 *
 * @author Pavel Zlámal <zlamal@cesnet.cz>
 */
public class PerunPwdReset extends AbstractPresenterModule implements EntryPoint{

	@Override
	protected void configure() {

		install(new DefaultModule.Builder().placeManager(PerunPlaceManager.class).build());

		// make sure app is embedded in a correct DIV
		bind(RootPresenter.class).to(PerunRootPresenter.class).asEagerSingleton();

		// Main Application must bind generic Presenter or its subclass and custom View !!
		bindPresenter(PerunPwdResetPresenter.class, PerunPwdResetPresenter.MyView.class, PerunPwdResetView.class, PerunPwdResetPresenter.MyProxy.class);

		// bind app-specific pages
		bindPresenter(PwdResetPresenter.class, PwdResetPresenter.MyView.class, PwdResetView.class, PwdResetPresenter.MyProxy.class);

		// pre-defined places
		bindConstant().annotatedWith(DefaultPlace.class).to(PerunPwdResetPlaceTokens.RESET);
		bindConstant().annotatedWith(ErrorPlace.class).to(PerunPwdResetPlaceTokens.NOT_FOUND);
		bindConstant().annotatedWith(UnauthorizedPlace.class).to(PerunPwdResetPlaceTokens.UNAUTHORIZED);

		// generic pages
		bindPresenter(NotAuthorizedPresenter.class, NotAuthorizedPresenter.MyView.class, NotAuthorizedView.class, NotAuthorizedPresenter.MyProxy.class);
		bindPresenter(NotFoundPresenter.class, NotFoundPresenter.MyView.class, NotFoundView.class, NotFoundPresenter.MyProxy.class);
		bindPresenter(LogoutPresenter.class, LogoutPresenter.MyView.class, LogoutView.class, LogoutPresenter.MyProxy.class);
		bindPresenter(NotUserPresenter.class, NotUserPresenter.MyView.class, NotUserView.class, NotUserPresenter.MyProxy.class);

	}

	@Override
	public void onModuleLoad() {

		ExceptionLogger exceptionHandler = new ExceptionLogger();
		GWT.setUncaughtExceptionHandler(exceptionHandler);

		try {

			// set default for Growl plugin
			Utils.getDefaultNotifyOptions().makeDefault();

			// ensure injecting custom CSS styles of PerunWui
			PerunResources.INSTANCE.gss().ensureInjected();

			PerunPwdResetResources.INSTANCE.gss().ensureInjected();

		} catch (Exception ex) {
			exceptionHandler.onUncaughtException(ex);
		}

	}

}