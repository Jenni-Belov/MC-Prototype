package ch.ethz.mc.ui.components.main_view.interventions.variables;

import java.io.File;

/* ##LICENSE## */
import org.bson.types.ObjectId;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanContainer;
import com.vaadin.data.util.BeanItem;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;

import ch.ethz.mc.conf.AdminMessageStrings;
import ch.ethz.mc.model.persistent.Intervention;
import ch.ethz.mc.model.persistent.InterventionVariableWithValue;
import ch.ethz.mc.model.persistent.types.InterventionVariableWithValueAccessTypes;
import ch.ethz.mc.model.persistent.types.InterventionVariableWithValuePrivacyTypes;
import ch.ethz.mc.model.ui.UIInterventionVariable;
import ch.ethz.mc.tools.CSVImporter;
import ch.ethz.mc.ui.NotificationMessageException;
import ch.ethz.mc.ui.components.basics.FileUploadComponentWithController;
import ch.ethz.mc.ui.components.basics.ShortStringEditComponent;
import ch.ethz.mc.ui.components.basics.FileUploadComponentWithController.UploadListener;
import lombok.val;
import lombok.extern.log4j.Log4j2;

/**
 * Extends the intervention variables tab component with a controller
 *
 * @author Andreas Filler
 */
@SuppressWarnings("serial")
@Log4j2
public class VariablesTabComponentWithController extends VariablesTabComponent {

	private final Intervention										intervention;

	private UIInterventionVariable									selectedUIVariable			= null;
	private BeanItem<UIInterventionVariable>						selectedUIVariableBeanItem	= null;

	private final BeanContainer<ObjectId, UIInterventionVariable>	beanContainer;

	public VariablesTabComponentWithController(
			final Intervention intervention) {
		super();

		this.intervention = intervention;

		// table options
		val interventionVariablesEditComponent = getVariablesEditComponent();
		val variablesTable = interventionVariablesEditComponent
				.getVariablesTable();

		// table content
		val variablesOfIntervention = getInterventionAdministrationManagerService()
				.getAllInterventionVariablesOfIntervention(
						intervention.getId());

		beanContainer = createBeanContainerForModelObjects(
				UIInterventionVariable.class, variablesOfIntervention);

		variablesTable.setContainerDataSource(beanContainer);
		variablesTable.setSortContainerPropertyId(
				UIInterventionVariable.getSortColumn());
		variablesTable
				.setVisibleColumns(UIInterventionVariable.getVisibleColumns());
		variablesTable
				.setColumnHeaders(UIInterventionVariable.getColumnHeaders());

		// handle table selection change
		variablesTable.addValueChangeListener(new ValueChangeListener() {

			@Override
			public void valueChange(final ValueChangeEvent event) {
				val objectId = variablesTable.getValue();
				if (objectId == null) {
					interventionVariablesEditComponent.setNothingSelected();
					selectedUIVariable = null;
					selectedUIVariableBeanItem = null;
				} else {
					selectedUIVariable = getUIModelObjectFromTableByObjectId(
							variablesTable, UIInterventionVariable.class,
							objectId);
					selectedUIVariableBeanItem = getBeanItemFromTableByObjectId(
							variablesTable, UIInterventionVariable.class,
							objectId);
					interventionVariablesEditComponent.setSomethingSelected();
				}
			}
		});

		// handle buttons
		val buttonClickListener = new ButtonClickListener();
		interventionVariablesEditComponent.getNewButton()
				.addClickListener(buttonClickListener);
		interventionVariablesEditComponent.getImportButton()
		.addClickListener(buttonClickListener);
		interventionVariablesEditComponent.getRenameButton()
				.addClickListener(buttonClickListener);
		interventionVariablesEditComponent.getEditButton()
				.addClickListener(buttonClickListener);
		interventionVariablesEditComponent.getSwitchPrivacyButton()
				.addClickListener(buttonClickListener);
		interventionVariablesEditComponent.getSwitchAccessButton()
				.addClickListener(buttonClickListener);
		interventionVariablesEditComponent.getDeleteButton()
				.addClickListener(buttonClickListener);
	}

	private class ButtonClickListener implements Button.ClickListener {
		@Override
		public void buttonClick(final ClickEvent event) {
			val accessControlEditComponent = getVariablesEditComponent();

			if (event.getButton() == accessControlEditComponent
					.getNewButton()) {
				createVariable();
			} else if (event.getButton() == accessControlEditComponent
					.getImportButton()) {
				importVariable();
			} else if (event.getButton() == accessControlEditComponent
					.getRenameButton()) {
				renameVariable();
			} else if (event.getButton() == accessControlEditComponent
					.getEditButton()) {
				editVariableValue();
			} else if (event.getButton() == accessControlEditComponent
					.getSwitchPrivacyButton()) {
				switchVariablePrivacyType();
			} else if (event.getButton() == accessControlEditComponent
					.getSwitchAccessButton()) {
				switchVariableAccessType();
			} else if (event.getButton() == accessControlEditComponent
					.getDeleteButton()) {
				deleteVariable();
			}
		}
	}
	
	public void importVariable() {
		
		log.debug("Import i18n CSV Variable");

		val fileUploadComponentWithController = new FileUploadComponentWithController(
				".csv");
		fileUploadComponentWithController.setListener(new UploadListener() {
			@Override
			public void fileUploadReceived(final File file) {
				log.debug(
						"File upload successful, starting conversation of i18n string objects for variables");
				try {
					val i18nStringObjects = CSVImporter
							.convertCSVToI18nVariablesStringsObjects(
									file);

					int updates = 0;

					log.debug("{} objects found in i18n import",
							i18nStringObjects.size());

					for (val i18nStringObject : i18nStringObjects) {
						val typeAndId = i18nStringObject.getId().split("_");
						val type = typeAndId[0];
						val i18nIdentifier = typeAndId[1];
						val check = typeAndId[2];

						if (check.equals("#")) {
								case "mm":
									updates += ias.variable18n(
											intervention.getId(),
											i18nIdentifier,
											i18nStringObject.getName(),
											i18nStringObject.getValue(),
											i18nStringObject.getPrivacy(),
											i18nStringObject.getAcess());
									break;
						}
					}

					log.debug("{} objects updated", updates);
					getAdminUI().showInformationNotification(
							AdminMessageStrings.NOTIFICATION__I18N_IMPORTED);
				} catch (final Exception e) {
					getAdminUI().showWarningNotification(
							AdminMessageStrings.NOTIFICATION__I18N_IMPORT_FAILED);
				} finally {
					try {
						file.delete();
					} catch (final Exception f) {
						// Do nothing
					}
				}
			}
		});
		showModalClosableEditWindow(
				AdminMessageStrings.ABSTRACT_CLOSABLE_EDIT_WINDOW__IMPORT_I18N,
				fileUploadComponentWithController, null);
	}

	public void createVariable() {
		log.debug("Create variable");
		showModalStringValueEditWindow(
				AdminMessageStrings.ABSTRACT_STRING_EDITOR_WINDOW__ENTER_NAME_FOR_VARIABLE,
				null, null, new ShortStringEditComponent(),
				new ExtendableButtonClickListener() {
					@Override
					public void buttonClick(final ClickEvent event) {
						InterventionVariableWithValue newVariable;
						try {
							
							
							// Create new variable
							newVariable = getInterventionAdministrationManagerService()
									.interventionVariableWithValueCreate(
											getStringValue(),
											intervention.getId());
						} catch (final Exception e) {
							handleException(e);
							return;
						}

						// Adapt UI
						
						for () {
							beanContainer.addItem(newVariable.getId(),
								UIInterventionVariable.class
										.cast(newVariable.toUIModelObject()));
						getVariablesEditComponent().getVariablesTable()
								.select(newVariable.getId());
						}
						
						getAdminUI().showInformationNotification(
								AdminMessageStrings.NOTIFICATION__VARIABLE_CREATED);

						closeWindow();
					}
				}, null);
	}

	public void renameVariable() {
		log.debug("Rename variable");

		showModalStringValueEditWindow(
				AdminMessageStrings.ABSTRACT_STRING_EDITOR_WINDOW__ENTER_NEW_NAME_FOR_VARIABLE,
				selectedUIVariable.getRelatedModelObject(
						InterventionVariableWithValue.class).getName(),
				null, new ShortStringEditComponent(),
				new ExtendableButtonClickListener() {
					@Override
					public void buttonClick(final ClickEvent event) {
						try {
							val selectedVariable = selectedUIVariable
									.getRelatedModelObject(
											InterventionVariableWithValue.class);

							// Change name
							getInterventionAdministrationManagerService()
									.interventionVariableWithValueSetName(
											selectedVariable, getStringValue());
						} catch (final Exception e) {
							handleException(e);
							return;
						}

						// Adapt UI
						getStringItemProperty(selectedUIVariableBeanItem,
								UIInterventionVariable.NAME)
										.setValue(selectedUIVariable
												.getRelatedModelObject(
														InterventionVariableWithValue.class)
												.getName());

						getAdminUI().showInformationNotification(
								AdminMessageStrings.NOTIFICATION__VARIABLE_RENAMED);
						closeWindow();
					}
				}, null);
	}

	public void switchVariablePrivacyType() {
		log.debug("Switch variable privacy type");

		val selectedVariable = selectedUIVariable
				.getRelatedModelObject(InterventionVariableWithValue.class);

		InterventionVariableWithValuePrivacyTypes newTypeValue = null;
		switch (selectedVariable.getPrivacyType()) {
			case PRIVATE:
				newTypeValue = InterventionVariableWithValuePrivacyTypes.SHARED_WITH_GROUP;
				break;
			case SHARED_WITH_GROUP:
				newTypeValue = InterventionVariableWithValuePrivacyTypes.SHARED_WITH_INTERVENTION;
				break;
			case SHARED_WITH_INTERVENTION:
				newTypeValue = InterventionVariableWithValuePrivacyTypes.SHARED_WITH_INTERVENTION_AND_DASHBOARD;
				break;
			case SHARED_WITH_INTERVENTION_AND_DASHBOARD:
				newTypeValue = InterventionVariableWithValuePrivacyTypes.PRIVATE;
				break;
		}

		try {
			getInterventionAdministrationManagerService()
					.interventionVariableWithValueSetPrivacyType(
							selectedVariable, newTypeValue);

			// Adapt UI
			getStringItemProperty(selectedUIVariableBeanItem,
					UIInterventionVariable.PRIVACY_TYPE)
							.setValue(selectedUIVariable
									.getRelatedModelObject(
											InterventionVariableWithValue.class)
									.getPrivacyType().toString());

			getAdminUI().showInformationNotification(
					AdminMessageStrings.NOTIFICATION__VARIABLE_SETTING_CHANGED);
		} catch (final NotificationMessageException e) {
			handleException(e);
		}
	}

	public void switchVariableAccessType() {
		log.debug("Switch variable privacy type");

		val selectedVariable = selectedUIVariable
				.getRelatedModelObject(InterventionVariableWithValue.class);

		InterventionVariableWithValueAccessTypes newTypeValue = null;
		switch (selectedVariable.getAccessType()) {
			case INTERNAL:
				newTypeValue = InterventionVariableWithValueAccessTypes.MANAGEABLE_BY_SERVICE;
				break;
			case MANAGEABLE_BY_SERVICE:
				newTypeValue = InterventionVariableWithValueAccessTypes.EXTERNALLY_READABLE;
				break;
			case EXTERNALLY_READABLE:
				newTypeValue = InterventionVariableWithValueAccessTypes.EXTERNALLY_READ_AND_WRITABLE;
				break;
			case EXTERNALLY_READ_AND_WRITABLE:
				newTypeValue = InterventionVariableWithValueAccessTypes.INTERNAL;
				break;
			default:
				break;
		}

		try {
			getInterventionAdministrationManagerService()
					.interventionVariableWithValueSetAccessType(
							selectedVariable, newTypeValue);

			// Adapt UI
			getStringItemProperty(selectedUIVariableBeanItem,
					UIInterventionVariable.ACCESS_TYPE)
							.setValue(selectedUIVariable
									.getRelatedModelObject(
											InterventionVariableWithValue.class)
									.getAccessType().toString());

			getAdminUI().showInformationNotification(
					AdminMessageStrings.NOTIFICATION__VARIABLE_SETTING_CHANGED);
		} catch (final NotificationMessageException e) {
			handleException(e);
		}
	}

	public void editVariableValue() {
		log.debug("Edit variable value");

		showModalStringValueEditWindow(
				AdminMessageStrings.ABSTRACT_STRING_EDITOR_WINDOW__ENTER_NEW_VALUE_FOR_VARIABLE,
				selectedUIVariable.getRelatedModelObject(
						InterventionVariableWithValue.class).getValue(),
				null, new ShortStringEditComponent(),
				new ExtendableButtonClickListener() {
					@Override
					public void buttonClick(final ClickEvent event) {
						try {
							val selectedVariable = selectedUIVariable
									.getRelatedModelObject(
											InterventionVariableWithValue.class);

							// Change name
							getInterventionAdministrationManagerService()
									.interventionVariableWithValueSetValue(
											selectedVariable, getStringValue());
						} catch (final Exception e) {
							handleException(e);
							return;
						}

						// Adapt UI
						getStringItemProperty(selectedUIVariableBeanItem,
								UIInterventionVariable.VALUE)
										.setValue(selectedUIVariable
												.getRelatedModelObject(
														InterventionVariableWithValue.class)
												.getValue());

						getAdminUI().showInformationNotification(
								AdminMessageStrings.NOTIFICATION__VARIABLE_VALUE_CHANGED);
						closeWindow();
					}
				}, null);
	}

	public void deleteVariable() {
		log.debug("Delete variable");
		showConfirmationWindow(new ExtendableButtonClickListener() {

			@Override
			public void buttonClick(final ClickEvent event) {
				try {
					val selectedVariable = selectedUIVariable
							.getRelatedModelObject(
									InterventionVariableWithValue.class);

					// Delete variable
					getInterventionAdministrationManagerService()
							.interventionVariableWithValueDelete(
									selectedVariable);
				} catch (final Exception e) {
					closeWindow();
					handleException(e);
					return;
				}

				// Adapt UI
				getVariablesEditComponent().getVariablesTable()
						.removeItem(selectedUIVariable
								.getRelatedModelObject(
										InterventionVariableWithValue.class)
								.getId());
				getAdminUI().showInformationNotification(
						AdminMessageStrings.NOTIFICATION__VARIABLE_DELETED);

				closeWindow();
			}
		}, null);
	}

}
