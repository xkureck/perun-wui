package cz.metacentrum.perun.wui.profile.pages;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.ViewWithUiHandlers;
import cz.metacentrum.perun.wui.model.PerunException;
import cz.metacentrum.perun.wui.model.beans.Attribute;
import cz.metacentrum.perun.wui.model.beans.RichMember;
import cz.metacentrum.perun.wui.model.beans.RichUser;
import cz.metacentrum.perun.wui.model.beans.Vo;
import cz.metacentrum.perun.wui.model.columnProviders.OverviewAttributesColumnProvider;
import cz.metacentrum.perun.wui.profile.client.resources.PerunProfileTranslation;
import cz.metacentrum.perun.wui.widgets.PerunDataGrid;
import cz.metacentrum.perun.wui.widgets.PerunLoader;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Column;
import org.gwtbootstrap3.client.ui.Heading;
import org.gwtbootstrap3.client.ui.constants.HeadingSize;
import org.gwtbootstrap3.client.ui.html.Div;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Vojtech Sassmann &lt;vojtech.sassmann@gmail.com&gt;
 */
public class CompleteInfoView extends ViewWithUiHandlers<CompleteInfoUiHandlers> implements CompleteInfoPresenter.MyView {

	interface GroupsViewUiBinder extends UiBinder<Widget, CompleteInfoView> {}

	private PerunProfileTranslation translation = GWT.create(PerunProfileTranslation.class);

	@UiField Heading userLabel;
	@UiField Heading voDataLabel;
	@UiField Button backButton;
	@UiField Column membersColumn;
	@UiField Column userColumn;
	@UiField PerunLoader loader;

	@UiField(provided = true)
	PerunDataGrid<Attribute> userDataGrid = new PerunDataGrid<>(new OverviewAttributesColumnProvider());

	@Inject
	public CompleteInfoView(GroupsViewUiBinder binder) {
		initWidget(binder.createAndBindUi(this));

		userDataGrid.setVisible(true);
		userDataGrid.setSelectionEnabled(false);
		userDataGrid.drawTableColumns();

		backButton.setText(translation.back());
		userLabel.setText(translation.userInfo());
		voDataLabel.setText(translation.voInfo());
	}

	@Override
	public void setUser(RichUser user) {
		List<Attribute> notEmptyAttributes = filterEmptyAttributes(user.getUserAttributes());
		userDataGrid.setList(notEmptyAttributes);

		userColumn.setVisible(true);
	}

	@Override
	public void onLoadingError(PerunException ex) {
		loader.onError(ex, event -> getUiHandlers().loadUserData());
	}

	@Override
	public void onLoadingStart() {
		userColumn.setVisible(false);
		membersColumn.setVisible(false);

		loader.setVisible(true);
		loader.onLoading(translation.loadingUserData());
	}

	@Override
	public void setMembers(Map<RichMember, Vo> memberVoMap) {
		membersColumn.clear();

		for (RichMember member : memberVoMap.keySet()) {
			Heading voHeading = new Heading(HeadingSize.H4, memberVoMap.get(member).getName());

			PerunDataGrid<Attribute> memberDataGrid = new PerunDataGrid<>(new OverviewAttributesColumnProvider());
			memberDataGrid.setVisible(true);
			memberDataGrid.setList(member.getMemberAttributes());
			memberDataGrid.setBordered(true);
			memberDataGrid.setCondensed(true);
			memberDataGrid.setStriped(true);
			memberDataGrid.setHover(true);
			memberDataGrid.setHeight("300px");
			memberDataGrid.setWidth("100%");
			memberDataGrid.setSelectionEnabled(false);
			memberDataGrid.drawTableColumns();

			Div memberArea = new Div();
			memberArea.add(voHeading);
			memberArea.add(memberDataGrid);

			membersColumn.add(memberArea);
		}

		loader.onFinished();
		loader.setVisible(false);
		membersColumn.setVisible(true);
	}

	@UiHandler({"backButton"})
	public void onBackClick(ClickEvent e) {
		getUiHandlers().navigateBack();
	}

	/**
	 * Keeps only attributes with not empty value.
	 *
	 * @param attributes Attributes to be filtered
	 * @return List of Attributes with not empty value
	 */
	private List<Attribute> filterEmptyAttributes(List<Attribute> attributes) {
		return attributes.stream()
				.filter((attribute -> !attribute.isEmpty()))
				.collect(Collectors.toList());
	}
}
