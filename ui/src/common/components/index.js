import AddItems from './AddItems.vue';
import AddSpecimens from './AddSpecimens.vue';
import AuditOverview from './AuditOverview.vue';
import Avatar from './Avatar.vue';
import BooleanCheckbox from './BooleanCheckbox.vue';
import Breadcrumb from './Breadcrumb.vue';
import Button from './Button.vue';
import ButtonGroup from './ButtonGroup.vue';
import ButtonLink from './ButtonLink.vue';
import Checkbox from './Checkbox.vue';
import Col from './Col.vue';
import Confirm from './Confirm.vue';
import ConfirmDelete from './ConfirmDelete.vue';
import ContainerPositionSelector from './ContainerPositionSelector.vue';
import ContainerSelector from './ContainerSelector.vue';
import DatePicker from './DatePicker.vue';
import DeleteObject from './DeleteObject.vue';
import Dialog from './Dialog.vue';
import Divider from './Divider.vue';
import Dropdown from './Dropdown.vue';
import FileUpload from './FileUpload.vue';
import Form from './Form.vue';
import FormGroup from './FormGroup.vue';
import Grid from './Grid.vue';
import GridColumn from './GridColumn.vue';
import Icon from './Icon.vue';
import IconTitle from './IconTitle.vue';
import InlineMessage from './InlineMessage.vue';
import InputNumber from './InputNumber.vue';
import InputText from './InputText.vue';
import ItemsValidationDialog from './ItemsValidationDialog.vue';
import Label from './Label.vue';
import ListGroup from './ListGroup.vue';
import ListSize from './ListSize.vue';
import ListView from './ListView.vue';
import Menu from './Menu.vue';
import Message from './Message.vue';
import MultiSelectDropdown from './MultiSelectDropdown.vue';
import Overlay from './Overlay.vue';
import Overview from './Overview.vue';
import Page from './Page.vue';
import PageBody from './PageBody.vue';
import PageHeader from './PageHeader.vue';
import PageToolbar from './PageToolbar.vue';
import Pager from './Pager.vue';
import Panel from './Panel.vue';
import Password from './Password.vue';
import PluginViews from './PluginViews.vue';
import ProgressBar from './ProgressBar.vue';
import PvDropdown from './PvDropdown.vue';
import RadioButton from './RadioButton.vue';
import Section from './Section.vue';
import SideMenu from './SideMenu.vue';
import SignaturePad from './SignaturePad.vue';
import SiteDropdown from './SiteDropdown.vue';
import Span from './Span.vue';
import SpecimenMeasure from './SpecimenMeasure.vue';
import Steps from './Steps.vue';
import Step from './Step.vue';
import StorageContainerDropdown from './StorageContainerDropdown.vue';
import StoragePosition from './StoragePosition.vue';
import StoragePositionSelector from './StoragePositionSelector.vue';
import Subform from './Subform.vue';
import TableForm from './TableForm.vue';
import Tag from './Tag.vue';
import Textarea from './Textarea.vue';
import TabView from './TabView.vue';
import TabPanel from './TabPanel.vue';
import UserDropdown from './UserDropdown.vue';
import UsernameAvatar from './UsernameAvatar.vue';

export default {
  install(app) {
    app.component('os-add-items',        AddItems);
    app.component('os-add-specimens',    AddSpecimens);
    app.component('os-audit-overview',   AuditOverview);
    app.component('os-avatar',           Avatar);
    app.component('os-boolean-checkbox', BooleanCheckbox);
    app.component('os-breadcrumb',       Breadcrumb);
    app.component('os-button',           Button);
    app.component('os-button-group',     ButtonGroup);
    app.component('os-button-link',      ButtonLink);
    app.component('os-checkbox',         Checkbox);
    app.component('os-column',           Col);
    app.component('os-confirm',          Confirm);
    app.component('os-container-selector', ContainerSelector);
    app.component('os-container-position-selector', ContainerPositionSelector);
    app.component('os-confirm-delete',   ConfirmDelete);
    app.component('os-date-picker',      DatePicker);
    app.component('os-delete-object',    DeleteObject);
    app.component('os-dialog',           Dialog);
    app.component('os-divider',          Divider);
    app.component('os-dropdown',         Dropdown);
    app.component('os-file-upload',      FileUpload);
    app.component('os-form',             Form);
    app.component('os-form-group',       FormGroup);
    app.component('os-grid',             Grid);
    app.component('os-grid-column',      GridColumn);
    app.component('os-icon',             Icon);
    app.component('os-icon-title',       IconTitle);
    app.component('os-inline-message',   InlineMessage);
    app.component('os-input-number',     InputNumber);
    app.component('os-input-text',       InputText);
    app.component('os-items-validation', ItemsValidationDialog);
    app.component('os-label',            Label);
    app.component('os-list-group',       ListGroup);
    app.component('os-list-size',        ListSize);
    app.component('os-list-view',        ListView);
    app.component('os-menu',             Menu);
    app.component('os-message',          Message);
    app.component('os-multi-select-dropdown', MultiSelectDropdown);
    app.component('os-overlay',          Overlay);
    app.component('os-overview',         Overview);
    app.component('os-page',             Page);
    app.component('os-page-body',        PageBody);
    app.component('os-page-head',        PageHeader);
    app.component('os-page-toolbar',     PageToolbar);
    app.component('os-pager',            Pager);
    app.component('os-panel',            Panel);
    app.component('os-password',         Password);
    app.component('os-plugin-views',     PluginViews);
    app.component('os-progress-bar',     ProgressBar);
    app.component('os-pv-dropdown',      PvDropdown);
    app.component('os-radio-button',     RadioButton);
    app.component('os-section',          Section);
    app.component('os-side-menu',        SideMenu);
    app.component('os-signature-pad',    SignaturePad);
    app.component('os-site-dropdown',    SiteDropdown);
    app.component('os-span',             Span);
    app.component('os-specimen-measure', SpecimenMeasure);
    app.component('os-containers-dropdown', StorageContainerDropdown);
    app.component('os-storage-position', StoragePosition);
    app.component('os-storage-position-selector', StoragePositionSelector);
    app.component('os-subform',          Subform);
    app.component('os-table-form',       TableForm);
    app.component('os-tag',              Tag);
    app.component('os-textarea',         Textarea);
    app.component('os-tabs',             TabView);
    app.component('os-tab',              TabPanel);
    app.component('os-username-avatar',  UsernameAvatar);
    app.component('os-user-dropdown',    UserDropdown);
    app.component('os-steps',            Steps);
    app.component('os-step',             Step);
  }
}