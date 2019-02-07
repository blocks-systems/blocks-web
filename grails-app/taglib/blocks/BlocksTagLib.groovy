package blocks

import blocks.organization.OrgUnit
import blocks.organization.OrgUnitController
import grails.plugin.springsecurity.SpringSecurityUtils
import groovy.xml.MarkupBuilder
import org.apache.commons.lang3.StringUtils
import org.apache.commons.logging.LogFactory
import org.springframework.web.servlet.support.RequestContextUtils

import java.text.DecimalFormat
import java.text.SimpleDateFormat

/**
 * Taglibs for blocks
 *
 * @author emil.wesolowski
 *
 */
class BlocksTagLib {

    private static final log = LogFactory.getLog(this)

    static namespace = "blocks"

    private static final String DELETE_CALLBACK = "setTimeout(function(){location.reload();},3000);"
    private static final String REMOVE_FROM_TABLE_ONLY = "btn.parent().parent().remove();"

    enum DateFormats {
        DATE("yyyy-MM-dd", "YYYY-MM-DD"),
        DATETIME("yyyy-MM-dd HH:mm", "YYYY-MM-DD HH:mm"),
        TIME("HH:mm", "HH:mm");

        DateFormats(String groovyFormat, String javascriptFormat) {
            this.groovyFormat = groovyFormat
            this.javascriptFormat = javascriptFormat
        }

        private final String groovyFormat;
        private final String javascriptFormat;

        final String getGroovyFormat(){
            return groovyFormat
        }
        final String getJavaScriptFormatFormat(){
            return javascriptFormat
        }
    }

    /**
     * Select Taglib by chmielpiwny
     * @usages:
     * 1) hideShow - Boolean - param defining ajax show in modal box
     * 2) controller - controller which has action 'ajaxEdit' to load show/edit in modal box
     * 3) create - controller which has action 'ajaxCreate' to create new object in modal box
     *
     */
    def select = { attrs, body ->
        if (attrs.id == null) attrs.id = "select_" + attrs.hashCode()
        if (request.xhr) attrs.id += "_xhr"
        def error = StringUtils.isNotBlank(attrs.error) ? attrs.error : '';
        attrs.remove('error')
        def hideShow = (attrs.hideShow != null && Boolean.valueOf(attrs.hideShow)) ? true : false
        // if controller not provided hide show must be true
        if (attrs.controller == null && !hideShow && !request.xhr) hideShow = Boolean.TRUE

        final StringWriter sb = new StringWriter()
        def builder = new MarkupBuilder(sb)

        sb.append("<div class='" + error + "'>")

        if (attrs.noSelection == null && !"multiple".equalsIgnoreCase(attrs.multiple)) attrs.noSelection = ['' : g.message(code: "default.select.empty.value")]

        def origAttrsMinusCustom = new HashMap(attrs)

        def required = (attrs.required != null && Boolean.valueOf(attrs.required))
        origAttrsMinusCustom.remove('required')
        if (required){
            origAttrsMinusCustom['required'] = "required"
            origAttrsMinusCustom['oninvalid'] = "this.setCustomValidity('" << g.message(code: 'default.required') << "');"
            origAttrsMinusCustom['oninput'] = "this.setCustomValidity('');"
        }

        builder."div"('class': 'col-sm-2 control-label') {
            def labelCode = attrs.labelCode != null ? attrs.labelCode : 'label'
            def labelMsg = g.message(code: labelCode)
            if (required) labelMsg += ' *'
            label('for': attrs.id, labelMsg)
        }

        def saveAccess = true
        if (attrs.saveAccess != null) {
            origAttrsMinusCustom.remove('saveAccess')
            saveAccess = SpringSecurityUtils.ifAnyGranted(attrs.saveAccess)
        }
        if (!saveAccess) attrs.create = null

        if (attrs.create != null) origAttrsMinusCustom.remove('create')
        if (attrs.dialogTitle != null) origAttrsMinusCustom.remove('dialogTitle')
        if (attrs.labelCode != null) origAttrsMinusCustom.remove('labelCode')
        if (attrs.controller != null) origAttrsMinusCustom.remove('controller')
        if (attrs.hideShow != null) origAttrsMinusCustom.remove('hideShow')
        if (origAttrsMinusCustom['class'] != null) {
            origAttrsMinusCustom['class'] = origAttrsMinusCustom['class'] + " form-control"
        } else {
            origAttrsMinusCustom['class'] = "form-control"
        }

        def readOnly = (attrs.readOnly != null && Boolean.valueOf(attrs.readOnly)) ? true : false
        if (request.xhr) {
            origAttrsMinusCustom['style'] = "width:100%;"
        } else {
            def width = 100;
            if (!hideShow) width -= 10
            if (attrs.create != null) width -= 10
            origAttrsMinusCustom['style'] = "width:$width%;"
        }

        if (readOnly) {
            origAttrsMinusCustom['disabled'] = 'disabled'
            origAttrsMinusCustom.remove('readOnly')
        }

        def disabled = (attrs.disabled != null && Boolean.valueOf(attrs.disabled)) ? true : false
        if (readOnly) {
            origAttrsMinusCustom['disabled'] = 'disabled'
        }

        sb.append("<div class='col-sm-4'>").append(g.select(origAttrsMinusCustom))

        if (!request.xhr) {
            if (!hideShow){
                sb.append("<a class='create btn btn-info add-inline' ").append("id='show_").append(attrs.id).append("'").append("><span class='fa fa-edit'></span></a>")
            }
            if (attrs.create != null && !readOnly) {
                // button
                sb.append("<a class='create btn btn-primary add-inline' ").append("id='add_").append(attrs.id).append("'").append("><span class='fa fa-plus'></span></a>")
            }
        }

        sb.append("</div>")
        sb.append("</div>")

        // JavaScript Code
        def locale = RequestContextUtils.getLocale(request).toString().substring(0, 2)
        sb.append("<script>").append("\$(document).ready(function() {").append("\$('#$attrs.id').select2({language:'$locale'});")
        if (!request.xhr) {
            def dialogTitleCode = attrs.dialogTitle != null ? attrs.dialogTitle : attrs.controller
            if (!hideShow) {
                def saveLink = g.createLink(controller: attrs.controller, action: 'ajaxSave')
                sb.append("\$('#$attrs.id').on('change', function(event) {")
                        .append("if(\$(this).val() == ''){\$('#show_$attrs.id').addClass('disabled');}else{\$('#show_$attrs.id').removeClass('disabled');}")
                        .append("});")
                        .append("if(\$('#$attrs.id').val() == ''){\$('#show_$attrs.id').addClass('disabled');}else{\$('#show_$attrs.id').removeClass('disabled');}")

                sb.append(createAddAjaxForm('show_' + attrs.id, attrs.controller, g.createLink(controller: attrs.controller, action: 'ajaxEdit'), "\$('#$attrs.id').val()", saveLink, dialogTitleCode, attrs.id, false, saveAccess));
            }
            if (attrs.create != null) {
                def buttonId = "add_$attrs.id"
                def saveLink = g.createLink(controller: attrs.controller, action: 'ajaxSave')
                def link = g.createLink(controller: attrs.controller, action: 'ajaxCreate')
                sb.append(createAddAjaxForm(buttonId, attrs.controller, link, null, saveLink, dialogTitleCode, attrs.id, false, saveAccess));
            }
        }
        if (required) {
            sb.append("\$('#$attrs.id').on('change', function(event) {").append("if(\$(this).val()!='' && \$('#$attrs.id').prop('oninput')!==null){\$('#$attrs.id')[0].oninput();}").append("});")
        }
        if (readOnly) {
            sb.append("\$('#").append(attrs.id).append("')").append(".closest('form').on('submit', function(event) {")
            sb.append("\$('#").append(attrs.id).append("').attr('disabled', false);")
            sb.append("});")
        }
        sb.append("});").append("</script>")


        sb.flush()

        def sbout = sb.toString()

        out << sbout
    }

    /**
     * Select Taglib by chmielpiwny
     * @usages:
     * 1) hideShow - Boolean - param defining ajax show in modal box
     * 2) controller - controller which has action 'ajaxEdit' to load show/edit in modal box
     * 3) create - controller which has action 'ajaxCreate' to create new object in modal box
     *
     */
    def select2 = { attrs, body ->
        if (attrs.id == null) attrs.id = "select_" + attrs.hashCode()
        //if (request.xhr) attrs.id += "_xhr"
        def error = StringUtils.isNotBlank(attrs.error) ? attrs.error : '';
        attrs.remove('error')
        def hideShow = (attrs.hideShow != null && Boolean.valueOf(attrs.hideShow)) ? true : false
        // if controller not provided hide show must be true
        if (attrs.controller == null && !hideShow && !request.xhr) hideShow = Boolean.TRUE

        final StringWriter sb = new StringWriter()
        def builder = new MarkupBuilder(sb)

        sb.append("<div class='" + error + "'>")

        if (attrs.noSelection == null && !"multiple".equalsIgnoreCase(attrs.multiple)) attrs.noSelection = ['' : g.message(code: "default.select.empty.value")]

        def origAttrsMinusCustom = new HashMap(attrs)

        def required = (attrs.required != null && Boolean.valueOf(attrs.required))
        origAttrsMinusCustom.remove('required')
        if (required){
            origAttrsMinusCustom['required'] = "required"
            origAttrsMinusCustom['oninvalid'] = "this.setCustomValidity('" << g.message(code: 'default.required') << "');"
            origAttrsMinusCustom['oninput'] = "this.setCustomValidity('');"
        }

        builder."div"('class': 'col-sm-2 control-label') {
            def labelCode = attrs.labelCode != null ? attrs.labelCode : 'label'
            def labelMsg = g.message(code: labelCode)
            if (required) labelMsg += ' *'
            label('for': attrs.id, labelMsg)
        }

        def saveAccess = true
        if (attrs.saveAccess != null) {
            origAttrsMinusCustom.remove('saveAccess')
            saveAccess = SpringSecurityUtils.ifAnyGranted(attrs.saveAccess)
        }
        if (!saveAccess) attrs.create = null

        if (attrs.create != null) origAttrsMinusCustom.remove('create')
        if (attrs.dialogTitle != null) origAttrsMinusCustom.remove('dialogTitle')
        if (attrs.labelCode != null) origAttrsMinusCustom.remove('labelCode')
        if (attrs.controller != null) origAttrsMinusCustom.remove('controller')
        if (attrs.hideShow != null) origAttrsMinusCustom.remove('hideShow')
        if (origAttrsMinusCustom['class'] != null) {
            origAttrsMinusCustom['class'] = origAttrsMinusCustom['class'] + " form-control"
        } else {
            origAttrsMinusCustom['class'] = "form-control"
        }

        def readOnly = (attrs.readOnly != null && Boolean.valueOf(attrs.readOnly)) ? true : false
        if (request.xhr) {
            origAttrsMinusCustom['style'] = "width:100%;"
        } else {
            def width = 100;
            if (!hideShow) width -= 10
            if (attrs.create != null) width -= 10
            origAttrsMinusCustom['style'] = "width:$width%;"
        }

        if (readOnly) {
            origAttrsMinusCustom['disabled'] = 'disabled'
            origAttrsMinusCustom.remove('readOnly')
        }

        def disabled = (attrs.disabled != null && Boolean.valueOf(attrs.disabled)) ? true : false
        if (readOnly) {
            origAttrsMinusCustom['disabled'] = 'disabled'
        }

        sb.append("<div class='col-sm-4'>").append(g.select(origAttrsMinusCustom))

        if (!request.xhr) {
            if (!hideShow){
                sb.append("<a class='create btn btn-info add-inline' ").append("id='show_").append(attrs.id).append("'").append("><span class='fa fa-edit'></span></a>")
            }
            if (attrs.create != null && !readOnly) {
                // button
                sb.append("<a class='create btn btn-primary add-inline' ").append("id='add_").append(attrs.id).append("'").append("><span class='fa fa-plus'></span></a>")
            }
        }

        sb.append("</div>")
        sb.append("</div>")

        // JavaScript Code
        def locale = RequestContextUtils.getLocale(request).toString().substring(0, 2)
        sb.append("<script>").append("\$(document).ready(function() {")
        if (!request.xhr) {
            def dialogTitleCode = attrs.dialogTitle != null ? attrs.dialogTitle : attrs.controller
            if (!hideShow) {
                def saveLink = g.createLink(controller: attrs.controller, action: 'ajaxSave')
                sb.append("\$('#$attrs.id').on('change', function(event) {")
                        .append("if(\$(this).val() == ''){\$('#show_$attrs.id').addClass('disabled');}else{\$('#show_$attrs.id').removeClass('disabled');}")
                        .append("});")
                        .append("if(\$('#$attrs.id').val() == ''){\$('#show_$attrs.id').addClass('disabled');}else{\$('#show_$attrs.id').removeClass('disabled');}")

                sb.append(createAddAjaxForm('show_' + attrs.id, attrs.controller, g.createLink(controller: attrs.controller, action: 'ajaxEdit'), "\$('#$attrs.id').val()", saveLink, dialogTitleCode, attrs.id, false, saveAccess));
            }
            if (attrs.create != null) {
                def buttonId = "add_$attrs.id"
                def saveLink = g.createLink(controller: attrs.controller, action: 'ajaxSave')
                def link = g.createLink(controller: attrs.controller, action: 'ajaxCreate')
                sb.append(createAddAjaxForm(buttonId, attrs.controller, link, null, saveLink, dialogTitleCode, attrs.id, false, saveAccess));
            }
        }
        if (required) {
            sb.append("\$('#$attrs.id').on('change', function(event) {").append("if(\$(this).val()!='' && \$('#$attrs.id').prop('oninput')!==null){\$('#$attrs.id')[0].oninput();}").append("});")
        }
        if (readOnly) {
            sb.append("\$('#").append(attrs.id).append("')").append(".closest('form').on('submit', function(event) {")
            sb.append("\$('#").append(attrs.id).append("').attr('disabled', false);")
            sb.append("});")
        }
        sb.append("});").append("</script>")


        sb.flush()

        out << sb.toString()
    }

    /**
     * Datetimepicker Taglib by chmielpiwny
     *
     * @usages:
     * - Minimum setup: fieldName - used in name in input so must be required, beanName - is required when rendering inputOnly
     * - Available attributes:
     * 1) String fieldName (required) - name of input and used in identifier of DOM element if id it's not defined
     * 2) String beanName (optional) - beanName used in build labelCode and in identifier of DOM element if id it's not defined
     * 3) String error (optional) - if render parent div with given css class
     * 4) Boolean inputOnly (optional) - flag defining if skip rendering label (default false)
     * 5) String value (optional) - value default in input
     * 6) Boolean pickTime (optional) - flag defining if datepicker should pick time (default false)
     * 7) Boolean pickDate (optional) - flag defining if datepicker should pick date (default true if pickTime is not defined)
     * 8) Boolean inline (optional) - flag defining if rendering timepicker is rendered inline
     * 9) String id (optional) - DOM element identifier (div wrapper and input)
     * 10) Boolean renderIcon (optional) - flag defining if rendering icon of datetimepicker (default true)
     *
     */
    def datetimepicker = { attrs, body ->
        if (attrs.fieldName == null) throw new IllegalArgumentException("Attribute 'fieldName' must be defined!")

        def inputOnly = attrs.inputOnly != null ? Boolean.valueOf(attrs.inputOnly) : false
        if (!inputOnly){
            if (attrs.beanName == null) throw new IllegalArgumentException("Attribute 'beanName' must be defined!")
        }

        def error = StringUtils.isNotBlank(attrs.error) ? attrs.error : '';
        attrs.remove('error')

        def val = attrs.value;

        def beanName = attrs.beanName
        def fieldName = attrs.fieldName

        // picktime - by default false - override by define in tag
        def pickTime = attrs.pickTime != null ? Boolean.valueOf(attrs.pickTime) : false
        // pickdate - if picktime it's not defined and pickdate it's not defined it's true otherwise it's false or overrided by define in tag
        def pickDate = attrs.pickDate != null ? Boolean.valueOf(attrs.pickDate) : (attrs.pickTime != null ? false : true)
        // render side by side time and date picker
        def inline = attrs.inline != null ? Boolean.valueOf(attrs.inline) : false

        def renderIcon = attrs.renderIcon != null ? Boolean.valueOf(attrs.renderIcon) : true

        def identifier = attrs.id != null ? attrs.id : attrs.beanName + "_" + attrs.fieldName
        def inputId = "datetimepicker_" + identifier
        if (request.xhr) inputId += "_xhr"
        def wrapperId = "datetimepicker_wrapper_" + identifier

        def format = DateFormats.DATE;
        if (pickTime && pickDate) {
            format = DateFormats.DATETIME;
        } else if (pickTime && !pickDate){
            format = DateFormats.TIME;
        }

        def stringDate = "";
        if (val !=  null) stringDate = val.format(format.getGroovyFormat())


        def labelCode = attrs.labelCode != null ? attrs.labelCode : beanName + '.' + fieldName + '.' + 'label'

        def label = g.message(code: labelCode)

        def locale = RequestContextUtils.getLocale(request).toString().substring(0, 2)

        def disabled = (attrs.disabled != null && Boolean.valueOf(attrs.disabled)) ? 'disabled="true"' : ''

        def readOnly = (attrs.readOnly != null && Boolean.valueOf(attrs.readOnly)) ? 'readOnly="true"' : ''

        def required = (attrs.required != null && Boolean.valueOf(attrs.required))
        def requiredAttr = ''

        if (required){
            requiredAttr << 'required = "required" '
            requiredAttr << 'oninvalid = "this.setCustomValidity(\'' << g.message(code: 'default.required') << '\');"'
            requiredAttr << 'oninput = "this.setCustomValidity(\'\');"'
            label += ' *'
        }

        final StringBuilder sb = new StringBuilder();

        // HTML Code
        sb.append("<div class='" + error + "'>")

        if (!inputOnly) sb.append('<div class="col-sm-2 control-label"><label>' + label + '</label></div>')

        sb.append('<div class="col-sm-4">')

        if (renderIcon && StringUtils.isBlank(disabled) && StringUtils.isBlank(readOnly)) sb.append('<div class="input-group datetime datetimepicker" id="' + wrapperId + '">')

        sb.append('<input type="text" class="form-control" ' + requiredAttr + disabled + readOnly + ' name="' + fieldName + '" id="' + inputId + '" value="' + stringDate + '">')

        if (renderIcon && StringUtils.isBlank(disabled) && StringUtils.isBlank(readOnly)) sb.append('<span class="input-group-addon"><span class="fa fa-calendar"></span></span>')

        if (renderIcon && StringUtils.isBlank(disabled) && StringUtils.isBlank(readOnly)) sb.append('</div>')

        sb.append('</div>')
        sb.append('</div>')

        // JavaScript Code
        if (StringUtils.isBlank(disabled) && StringUtils.isBlank(readOnly))
            sb.append("<script>")
                    .append("\$(document).ready(function() {")
                    .append("\$('#").append(renderIcon ? wrapperId : inputId).append("').datetimepicker({")
                    .append("locale:'pl',")
                    //.append("widgetPositioning:{horizontal: 'auto',vertical: 'top'},")
                    .append("showClear:true,")
                    .append("showClose:true,")
                    //.append("sideBySide:true,")
                    .append("format:'").append(format.getJavaScriptFormatFormat()).append("',")
                    .append("inline:").append(inline)
                    .append("});")
                    .append("});")
                    .append("</script>")
        out << sb.toString();
    }

    /**
     * Datepicker Taglib by fgroch
     *
     * @usages:
     * - Minimum setup: fieldName - used in name in input so must be required, beanName - is required when rendering inputOnly
     * - Available attributes:
     * 1) String fieldName (required) - name of input and used in identifier of DOM element if id it's not defined
     * 2) String beanName (optional) - beanName used in build labelCode and in identifier of DOM element if id it's not defined
     * 3) String error (optional) - if render parent div with given css class
     * 4) Boolean inputOnly (optional) - flag defining if skip rendering label (default false)
     * 5) String value (optional) - value default in input
     * 6) String id (optional) - DOM element identifier (div wrapper and input)
     * 7) Boolean renderIcon (optional) - flag defining if rendering icon of datepicker (default true)
     * 8) String dateFormat - string format for date
     *
     */
    def datepicker = { attrs, body ->
        if (attrs.fieldName == null) throw new IllegalArgumentException("Attribute 'fieldName' must be defined!")

        def inputOnly = attrs.inputOnly != null ? Boolean.valueOf(attrs.inputOnly) : false
        if (!inputOnly){
            if (attrs.beanName == null) throw new IllegalArgumentException("Attribute 'beanName' must be defined!")
        }

        def error = StringUtils.isNotBlank(attrs.error) ? attrs.error : '';
        attrs.remove('error')

        def val = attrs.value;

        def beanName = attrs.beanName
        def fieldName = attrs.fieldName

        def renderIcon = attrs.renderIcon != null ? Boolean.valueOf(attrs.renderIcon) : true

        def identifier = attrs.id != null ? attrs.id : attrs.beanName + "_" + attrs.fieldName
        def inputId = "datepicker_" + identifier
        //if (request.xhr) inputId += "_xhr"
        def wrapperId = "datepicker_wrapper_" + identifier

        def format = attrs.dateFormat != null ? attrs.dateFormat : "yyyy-mm-dd"
        def formatLambda = "{toDisplay: function (date, format, language) {\n" +
                "           if (date === null) {" +
                "               return '';};" +
                "            var d = new Date(date);\n" +
                "            return d.toISOString();\n" +
                "        }}"
        def stringDate = ""
        if (val != null) {
            stringDate = new SimpleDateFormat("yyyy-MM-dd").format(val)
        } else {
            stringDate = ""
        }
        //def stringDate = "";
        //if (val !=  null) stringDate = val.format(format.getGroovyFormat())


        def labelCode = attrs.labelCode != null ? attrs.labelCode : beanName + '.' + fieldName + '.' + 'label'

        def label = g.message(code: labelCode)

        def locale = RequestContextUtils.getLocale(request).toString().substring(0, 2)

        def disabled = (attrs.disabled != null && Boolean.valueOf(attrs.disabled)) ? 'disabled="true"' : ''

        def readOnly = (attrs.readOnly != null && Boolean.valueOf(attrs.readOnly)) ? 'readOnly="true"' : ''

        def required = (attrs.required != null && Boolean.valueOf(attrs.required))
        def requiredAttr = ''

        if (required){
            requiredAttr << 'required = "required" '
            requiredAttr << 'oninvalid = "this.setCustomValidity(\'' << g.message(code: 'default.required') << '\');"'
            requiredAttr << 'oninput = "this.setCustomValidity(\'\');"'
            label += ' *'
        }

        final StringBuilder sb = new StringBuilder();

        // HTML Code
        sb.append("<div class='" + error + "'>")

        if (!inputOnly) sb.append('<div class="col-sm-2 control-label"><label>' + label + '</label></div>')

        sb.append('<div class="col-sm-4">')

        if (renderIcon && StringUtils.isBlank(disabled) && StringUtils.isBlank(readOnly)) sb.append('<div class="input-group date datepicker" id="' + wrapperId + '">')

        sb.append('<input type="text" class="form-control" ' + requiredAttr + disabled + readOnly + ' name="' + fieldName + '" id="' + inputId + '" value="' + stringDate + '">')

        if (renderIcon && StringUtils.isBlank(disabled) && StringUtils.isBlank(readOnly)) sb.append('<span class="input-group-addon"><span class="fa fa-calendar"></span></span>')

        if (renderIcon && StringUtils.isBlank(disabled) && StringUtils.isBlank(readOnly)) sb.append('</div>')

        sb.append('</div>')
        sb.append('</div>')

        // JavaScript Code
        if (StringUtils.isBlank(disabled) && StringUtils.isBlank(readOnly))
            sb.append("<script>")
                    .append("\$(document).ready(function() {")
                    .append("\$('#").append(renderIcon ? wrapperId : inputId).append("').datepicker({")
                    .append("language:'").append(locale).append("',")
                    .append("format:'").append(format).append("',")
                    //.append("format:").append(formatLambda).append(",")
                    .append("todayHighlight: true,")
                    .append("autoclose: true")
                    .append("});")
                    .append("});")
                    .append("</script>")
        out << sb.toString();
    }

    def beanErrors = { attrs ->
        def bean = attrs.remove('bean')
        if (bean) {
            out << "<div class='col-lg-12 center-block text-centered'>"
            out << "<div class='col-lg-6 alert alert-block alert-danger'>"
            out << "<p class='col-lg-12'><h4 style='font-weight: bold;'>" + g.message(code: 'bean.errors') + "</h4></p>"
            out << '<a class="close" data-dismiss="alert">&times;</a>'
            out << eachError(attrs, {
                out << '<p>' << g.message(error:it) << '</p>'
            })
            out << '</div>'
            out << '</div>'
        }
    }

    def field = { attrs, body ->
        if (attrs.id == null) attrs.id = "field_" + attrs.hashCode()
        //if (request.xhr) attrs.id += "_xhr"
        def error = StringUtils.isNotBlank(attrs.error) ? attrs.error : '';
        attrs.remove('error')
        def numeric = attrs.numeric != null ? Boolean.valueOf(attrs.numeric) : false
        attrs.remove('numeric')

        final StringWriter writer = new StringWriter()
        def builder = new MarkupBuilder(writer)

        writer.append("<div class='" + error + "'>")

        def origAttrsMinusCustom = new HashMap(attrs)

        def required = (attrs.required != null && Boolean.valueOf(attrs.required))
        origAttrsMinusCustom.remove('required')
        if (required && attrs.type != 'checkbox') {
            origAttrsMinusCustom['required'] = "required"
            origAttrsMinusCustom['oninvalid'] = "this.setCustomValidity('" << g.message(code: 'default.required') << "');"
            origAttrsMinusCustom['oninput'] = "this.setCustomValidity('');"
        }

        def extended = (attrs.extended != null && Boolean.valueOf(attrs.extended))
        origAttrsMinusCustom.remove('extended')

        builder."div"('class': 'col-sm-2 control-label') {
            def labelCode = attrs.labelCode != null ? attrs.labelCode : 'label'
            def labelMsg = g.message(code: labelCode)
            if (required && attrs.type != 'checkbox') labelMsg += ' *'
            label('for': attrs.id, labelMsg)
        }


        origAttrsMinusCustom.remove('labelCode')
        def readOnly = (attrs.readOnly != null && Boolean.valueOf(attrs.readOnly))
        origAttrsMinusCustom.remove('readOnly')

        if (attrs.type != 'checkbox') {
            if (origAttrsMinusCustom['class'] != null) {
                origAttrsMinusCustom['class'] = origAttrsMinusCustom['class'] + " form-control"
            } else {
                origAttrsMinusCustom['class'] = "form-control"
            }
        }
        if (readOnly) {
            origAttrsMinusCustom['readOnly'] = "readOnly"
        }


        if (extended) {
            writer.append("<div class='col-sm-10'>")
        } else {
            writer.append("<div class='col-sm-4'>")
        }
        if (attrs.type == 'checkbox') {
            if (origAttrsMinusCustom['style'] != null) {
                origAttrsMinusCustom['style'] = origAttrsMinusCustom['style'] + " width: auto;"
            } else {
                origAttrsMinusCustom['style'] = "width: auto;"
            }
            writer.append(g.checkBox(origAttrsMinusCustom))
        } else if (attrs.type == 'textarea') {
            writer.append(g.textArea(origAttrsMinusCustom))
        } else {
            writer.append(g.field(origAttrsMinusCustom))
        }

        writer.append(body().toString());

        writer.append("</div>")
        writer.append("</div>")

        //add javascript
        if (numeric) {
            final DecimalFormat formatter = (DecimalFormat) DecimalFormat.getInstance(RequestContextUtils.getLocale(request));
            def dSeparator = formatter.getDecimalFormatSymbols().getDecimalSeparator()
            def aSeparator = formatter.getDecimalFormatSymbols().getGroupingSeparator()
            writer.append("<script>").append("\$(document).ready(function() {")
                    .append("\$('#").append(attrs.id).append("').autoNumeric('init', {aSep: '$aSeparator', aDec: '$dSeparator'});")
                    .append("});").append("</script>")
        }

        writer.flush()
        out << writer.toString()
    }

    /**
     *
     */
    def autocomplete = { attrs, body ->
        if (attrs.id == null) attrs.id = "autocomplete_select_" + attrs.hashCode()
        if (request.xhr) attrs.id += "_xhr"
        def error = StringUtils.isNotBlank(attrs.error) ? attrs.error : '';
        attrs.remove('error')
        def hideShow = (attrs.hideShow != null && Boolean.valueOf(attrs.hideShow)) ? true : false
        // if controller not provided hide show must be true
        if (attrs.controller == null && !hideShow && !request.xhr) hideShow = Boolean.TRUE

        def multiple = attrs.multiple != null ? Boolean.valueOf(attrs.multiple) : false
        def pageLimit = attrs.pageLimit != null ? Integer.valueOf(attrs.pageLimit) : 10

        def origAttrsMinusCustom = new HashMap(attrs)

        def required = (attrs.required != null && Boolean.valueOf(attrs.required))
        origAttrsMinusCustom.remove('required')
        if (required){
            origAttrsMinusCustom['required'] = "required"
            origAttrsMinusCustom['oninvalid'] = "this.setCustomValidity('" << g.message(code: 'default.required') << "');"
            origAttrsMinusCustom['oninput'] = "this.setCustomValidity('');"
        }

        def saveAccess = true
        if (attrs.saveAccess != null) {
            origAttrsMinusCustom.remove('saveAccess')
            saveAccess = SpringSecurityUtils.ifAnyGranted(attrs.saveAccess)
        }
        if (!saveAccess) attrs.create = null

        // html
        final StringWriter sb = new StringWriter()
        def builder = new MarkupBuilder(sb)

        sb.append("<div class='" + error + "'>")

        builder."div"('class': 'col-sm-2 control-label') {
            def labelCode = attrs.labelCode != null ? attrs.labelCode : 'label'
            def labelMsg = g.message(code: labelCode)
            label('for': attrs.id, labelMsg)
        }

        if (attrs.minimumInputLength != null) origAttrsMinusCustom.remove('minimumInputLength')
        if (attrs.placeholderCode != null) origAttrsMinusCustom.remove('placeholderCode')
        if (attrs.callback != null) origAttrsMinusCustom.remove('callback')
        if (attrs.create != null) origAttrsMinusCustom.remove('create')
        if (attrs.dialogTitle != null) origAttrsMinusCustom.remove('dialogTitle')
        if (attrs.labelCode != null) origAttrsMinusCustom.remove('labelCode')
        if (attrs.controller != null) origAttrsMinusCustom.remove('controller')
        if (attrs.hideShow != null) origAttrsMinusCustom.remove('hideShow')
        if (origAttrsMinusCustom['class'] != null) {
            origAttrsMinusCustom['class'] = origAttrsMinusCustom['class'] + " form-control"
        } else {
            origAttrsMinusCustom['class'] = "form-control"
        }

        def readOnly = (attrs.readOnly != null && Boolean.valueOf(attrs.readOnly)) ? true : false
        def minimumInputLength = attrs.minimumInputLength != null ? attrs.minimumInputLength : 3
        def create = (attrs.create != null && Boolean.valueOf(attrs.create)) ? true : false
        if (request.xhr) {
            origAttrsMinusCustom['style'] = "width:100%;"
        } else {
            def width = 100;
            if (!hideShow) width -= 10
            if (create) width -= 10
            origAttrsMinusCustom['style'] = "width:$width%;"
        }

        if (readOnly) {
            origAttrsMinusCustom['disabled'] = 'disabled'
            origAttrsMinusCustom.remove('readOnly')
        }

        def disabled = (attrs.disabled != null && Boolean.valueOf(attrs.disabled)) ? true : false
        if (readOnly) {
            origAttrsMinusCustom['disabled'] = 'disabled'
        }

        sb.append("<div class='col-sm-4'>").append(g.field(origAttrsMinusCustom))

        if (!request.xhr) {
            if (!hideShow){
                sb.append("<a class='create btn btn-info add-inline' ").append("id='show_").append(attrs.id).append("'").append("><span class='fa fa-edit'></span></a>")
            }
            if (create && !readOnly) {
                // button
                sb.append("<a class='create btn btn-primary add-inline' ").append("id='add_").append(attrs.id).append("'").append("><span class='fa fa-plus'></span></a>")
            }
        }

        sb.append("</div>")
        sb.append("</div>")

        // javascript code
        def placeholderCode = attrs.placeholderCode != null ? attrs.placeholderCode : 'default.choose'

        def linkAttr = [:]
        linkAttr['controller'] = attrs.controller
        linkAttr['action'] = attrs.autocompleteAction != null ? attrs.autocompleteAction : 'autocomplete'

        def autocompleteLink = g.createLink(linkAttr)

        linkAttr = [:]
        linkAttr['controller'] = attrs.controller
        linkAttr['action'] = attrs.selectionAction != null ? attrs.selectionAction : 'findById'

        def selectionLink = g.createLink(linkAttr)
        def callback = StringUtils.isNotBlank(attrs.callback) ? attrs.callback : '';
        def locale = RequestContextUtils.getLocale(request).toString().substring(0, 2)
        sb.append("<script>").append("\$(document).ready(function() {")
                .append("\$('#$attrs.id').select2({")
                .append("initSelection:function(element,callback) {if(\$('#$attrs.id').val()!==''){\$.getJSON('$selectionLink/'+\$('#$attrs.id').val(),function(data){${callback}callback(data);});}},")
                .append("language:'$locale',allowClear:true,minimumInputLength:$minimumInputLength,multiple:$multiple,")
                .append("placeholder:'").append(g.message(code: placeholderCode)).append("',")
                .append("closeOnSelect:true,theme:'bootstrap',")
                .append("ajax:{cache:false,type:'GET',dataType:'JSON',url:'$autocompleteLink',quietMillis:500,data:function(term,page){return {term:term,offset:page-1,limit:$pageLimit};},results:function(data,page){return {results:data.items,more:(page*$pageLimit)<data.total};}}")
                .append("});")

        if (!request.xhr) {
            def dialogTitleCode = attrs.dialogTitle != null ? attrs.dialogTitle : attrs.controller
            if (!hideShow) {
                def saveLink = g.createLink(controller: attrs.controller, action: 'ajaxSave')
                sb.append("\$('#$attrs.id').on('change', function(event) {")
                        .append("if(\$(this).val() == ''){\$('#show_$attrs.id').addClass('disabled');}else{\$('#show_$attrs.id').removeClass('disabled');}")
                        .append("});")
                        .append("if(\$('#$attrs.id').val() == ''){\$('#show_$attrs.id').addClass('disabled');}else{\$('#show_$attrs.id').removeClass('disabled');}")
                sb.append(createAddAjaxForm('show_' + attrs.id, attrs.controller, g.createLink(controller: attrs.controller, action: 'ajaxEdit'), "\$('#$attrs.id').val()", saveLink, dialogTitleCode, attrs.id, true, saveAccess));
            }
            if (create) {
                def buttonId = "add_$attrs.id"
                def saveLink = g.createLink(controller: attrs.controller, action: 'ajaxSave')
                def link = g.createLink(controller: attrs.controller, action: 'ajaxCreate')
                sb.append(createAddAjaxForm(buttonId, attrs.controller, link, null, saveLink, dialogTitleCode, attrs.id, true, saveAccess));
            }
        }
        if (required) {
            sb.append("\$('#$attrs.id').on('change', function(event) {").append("if(\$(this).val()!='' && \$('#$attrs.id').prop('oninput')!==null){\$('#$attrs.id')[0].oninput();}").append("});")
        }
        if (readOnly) {
            sb.append("\$('#").append(attrs.id).append("')").append(".closest('form').on('submit', function(event) {")
            sb.append("\$('#").append(attrs.id).append("').attr('disabled', false);")
            sb.append("});")
        }

        sb.append("});").append("</script>")

        sb.flush()

        out << sb.toString()
    }

    def eventKindIconPicker = { attrs ->
        if (attrs.id == null) attrs.id = "field_" + attrs.hashCode()
        if (request.xhr) attrs.id += "_xhr"
        def error = StringUtils.isNotBlank(attrs.error) ? attrs.error : '';
        attrs.remove('error')

        final StringWriter sb = new StringWriter()
        def builder = new MarkupBuilder(sb)

        builder."div"('class': 'col-sm-2 control-label') {
            def labelCode = attrs.labelCode != null ? attrs.labelCode : 'label'
            def labelMsg = g.message(code: labelCode)
            label('for': attrs.id, labelMsg)
        }
        builder."div"('class': 'col-sm-4') {
            'input'('name':attrs.id, 'id':attrs.id, 'class':'form-control',
                    'type':'hidden', 'value':attrs.value ?: '')
            div('class':'btn-group') {
                button('class':'btn btn-primary iconpicker-component', 'type':'button') {
                    i('','class':attrs.value ?: 'fa fa-ellipsis-h')
                }
                button('class':'icp icp-dd btn btn-primary dropdown-toggle iconpicker-element',
                        'data-toggle':'dropdown', 'data-selected':'fa-car', 'type':'button') {
                    span('', 'class':'caret')
                    span('', 'class':'sr-only')
                }
                div('class':'dropdown-menu iconpicker-container') {
                    div('class':'iconpicker-popover popover fade in inline',
                            'style':'top: auto; right: auto; bottom: auto; left: auto; max-width: none;') {
                        div('', 'class':'arrow')
                        div('class':'popover-content') {
                            div('class':'iconpicker') {
                                div('class':'iconpicker-items') {
                                    a('class':'iconpicker-item', 'href':'#', 'role':'button',
                                            'title':'.fa-asterisk') {
                                        i('', 'class':'fa fa-asterisk')
                                    }
                                    a('class':'iconpicker-item', 'href':'#', 'role':'button', 'title':'.fa-plus') {
                                        i('', 'class':'fa fa-plus')
                                    }
                                    a('class':'iconpicker-item', 'href':'#', 'role':'button',
                                            'title':'.fa-edit') {
                                        i('', 'class':'fa fa-edit')
                                    }
                                    a('class':'iconpicker-item', 'href':'#', 'role':'button',
                                            'title':'.fa-thumb-tack') {
                                        i('','class':'fa fa-thumb-tack')
                                    }
                                    a('class':'iconpicker-item', 'href':'#', 'role':'button',
                                            'title':'.fa-paperclip') {
                                        i('','class':'fa fa-paperclip')
                                    }
                                    a('class':'iconpicker-item', 'href':'#', 'role':'button',
                                            'title':'.fa-exchange') {
                                        i('', 'class':'fa fa-exchange')
                                    }
                                    a('class':'iconpicker-item', 'href':'#', 'role':'button',
                                            'title':'.fa-exclamation') {
                                        i('', 'class':'fa fa-exclamation')
                                    }
                                    a('class':'iconpicker-item', 'href':'#', 'role':'button',
                                            'title':'.fa-info') {
                                        i('', 'class':'fa fa-info')
                                    }
                                    a('class':'iconpicker-item', 'href':'#', 'role':'button',
                                            'title':'.fa-money') {
                                        i('', 'class':'fa fa-money')
                                    }
                                    a('class':'iconpicker-item', 'href':'#', 'role':'button',
                                            'title':'.fa-archive') {
                                        i('', 'class':'fa fa-archive')
                                    }
                                    a('class':'iconpicker-item', 'href':'#', 'role':'button',
                                            'title':'.fa-recycle') {
                                        i('', 'class':'fa fa-recycle')
                                    }
                                    a('class':'iconpicker-item', 'href':'#', 'role':'button',
                                            'title':'.fa-lock') {
                                        i('', 'class':'fa fa-lock')
                                    }
                                    a('class':'iconpicker-item', 'href':'#', 'role':'button',
                                            'title':'.fa-pause') {
                                        i('', 'class':'fa fa-pause')
                                    }
                                    a('class':'iconpicker-item', 'href':'#', 'role':'button',
                                            'title':'.fa-play') {
                                        i('', 'class':'fa fa-play')
                                    }
                                    a('class':'iconpicker-item', 'href':'#', 'role':'button',
                                            'title':'.fa-stop') {
                                        i('', 'class':'fa fa-stop')
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // JavaScript Code
        sb.append("<script type='text/javascript'>").append("\$(function() {")
        sb.append("\$('.iconpicker-item').on('click', function(e) {")
        sb.append("\$('#statusIcon').val(\$(this).find('i').attr('class'));")
        sb.append("\$('.iconpicker-component').find('i').get(0).className = \$(this).find('i').attr('class');")
        sb.append("});});").append("</script>")

        sb.flush()

        out << sb.toString()
    }

    def statusIconPicker = { attrs ->
        if (attrs.id == null) attrs.id = "field_" + attrs.hashCode()
        if (request.xhr) attrs.id += "_xhr"
        def error = StringUtils.isNotBlank(attrs.error) ? attrs.error : '';
        attrs.remove('error')

        final StringWriter sb = new StringWriter()
        def builder = new MarkupBuilder(sb)

        builder."div"('class': 'col-sm-2 control-label') {
            def labelCode = attrs.labelCode != null ? attrs.labelCode : 'label'
            def labelMsg = g.message(code: labelCode)
            label('for': attrs.id, labelMsg)
        }
        builder."div"('class': 'col-sm-4') {
            'input'('name':attrs.id, 'id':attrs.id, 'class':'form-control',
                    'type':'hidden', 'value':attrs.value ?: '')
            div('class':'btn-group') {
                button('class':'btn btn-primary iconpicker-component', 'type':'button') {
                    i('','class':attrs.value ?: 'fa fa-ellipsis-h')
                }
                button('class':'icp icp-dd btn btn-primary dropdown-toggle iconpicker-element',
                        'data-toggle':'dropdown', 'data-selected':'fa-car', 'type':'button') {
                    span('', 'class':'caret')
                    span('', 'class':'sr-only')
                }
                div('class':'dropdown-menu iconpicker-container') {
                    div('class':'iconpicker-popover popover fade in inline',
                            'style':'top: auto; right: auto; bottom: auto; left: auto; max-width: none;') {
                        div('', 'class':'arrow')
                        div('class':'popover-content') {
                            div('class':'iconpicker') {
                                div('class':'iconpicker-items') {
                                    a('class':'iconpicker-item', 'href':'#', 'role':'button',
                                            'title':'.fa-asterisk') {
                                        i('', 'class':'fa fa-asterisk')
                                    }
                                    a('class':'iconpicker-item', 'href':'#', 'role':'button', 'title':'.fa-plus') {
                                        i('', 'class':'fa fa-plus')
                                    }
                                    a('class':'iconpicker-item', 'href':'#', 'role':'button',
                                            'title':'.fa-edit') {
                                        i('', 'class':'fa fa-edit')
                                    }
                                    a('class':'iconpicker-item', 'href':'#', 'role':'button',
                                            'title':'.fa-thumb-tack') {
                                        i('','class':'fa fa-thumb-tack')
                                    }
                                    a('class':'iconpicker-item', 'href':'#', 'role':'button',
                                            'title':'.fa-paperclip') {
                                        i('','class':'fa fa-paperclip')
                                    }
                                    a('class':'iconpicker-item', 'href':'#', 'role':'button',
                                            'title':'.fa-exchange') {
                                        i('', 'class':'fa fa-exchange')
                                    }
                                    a('class':'iconpicker-item', 'href':'#', 'role':'button',
                                            'title':'.fa-exclamation') {
                                        i('', 'class':'fa fa-exclamation')
                                    }
                                    a('class':'iconpicker-item', 'href':'#', 'role':'button',
                                            'title':'.fa-info') {
                                        i('', 'class':'fa fa-info')
                                    }
                                    a('class':'iconpicker-item', 'href':'#', 'role':'button',
                                            'title':'.fa-money') {
                                        i('', 'class':'fa fa-money')
                                    }
                                    a('class':'iconpicker-item', 'href':'#', 'role':'button',
                                            'title':'.fa-archive') {
                                        i('', 'class':'fa fa-archive')
                                    }
                                    a('class':'iconpicker-item', 'href':'#', 'role':'button',
                                            'title':'.fa-recycle') {
                                        i('', 'class':'fa fa-recycle')
                                    }
                                    a('class':'iconpicker-item', 'href':'#', 'role':'button',
                                            'title':'.fa-lock') {
                                        i('', 'class':'fa fa-lock')
                                    }
                                    a('class':'iconpicker-item', 'href':'#', 'role':'button',
                                            'title':'.fa-pause') {
                                        i('', 'class':'fa fa-pause')
                                    }
                                    a('class':'iconpicker-item', 'href':'#', 'role':'button',
                                            'title':'.fa-play') {
                                        i('', 'class':'fa fa-play')
                                    }
                                    a('class':'iconpicker-item', 'href':'#', 'role':'button',
                                            'title':'.fa-stop') {
                                        i('', 'class':'fa fa-stop')
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // JavaScript Code
        sb.append("<script type='text/javascript'>").append("\$(function() {")
        sb.append("\$('.iconpicker-item').on('click', function(e) {")
        sb.append("\$('#statusIcon').val(\$(this).find('i').attr('class'));")
        sb.append("\$('.iconpicker-component').find('i').get(0).className = \$(this).find('i').attr('class');")
        sb.append("});});").append("</script>")

        sb.flush()

        out << sb.toString()
    }

    def statusColorPicker = { attrs ->
        if (attrs.id == null) attrs.id = "field_" + attrs.hashCode()
        if (request.xhr) attrs.id += "_xhr"
        def error = StringUtils.isNotBlank(attrs.error) ? attrs.error : '';
        attrs.remove('error')
        if (attrs.value == null) attrs.value = ''

        final StringWriter sb = new StringWriter()
        def builder = new MarkupBuilder(sb)

        builder."div"('class': 'col-sm-2 control-label') {
            def labelCode = attrs.labelCode != null ? attrs.labelCode : 'label'
            def labelMsg = g.message(code: labelCode)
            label('for': attrs.id, labelMsg)
        }
        builder."div"('class': 'col-sm-4') {
            'input'('name':attrs.id, 'id':attrs.id, 'class':'form-control',
                    'type':'hidden', 'value':attrs.value)
            div('class':'btn-group', 'data-toggle':'buttons') {
                label('', 'class':attrs.value == 'primary' ? 'btn btn-primary status-color active' : 'btn btn-primary status-color') {
                    input('1','type':'radio', 'id':'primary', 'name':'options', 'autocomplete':'off')
                }
                label('', 'class':attrs.value == 'info' ? 'btn btn-info status-color active' : 'btn btn-info status-color') {
                    input('2','type':'radio', 'id':'info', 'name':'options', 'autocomplete':'off')
                }
                label('', 'class':attrs.value == 'success' ? 'btn btn-success status-color active' : 'btn btn-success status-color') {
                    input('3','type':'radio', 'id':'success', 'name':'options', 'autocomplete':'off')
                }
                label('', 'class':attrs.value == 'warning' ? 'btn btn-warning status-color active' : 'btn btn-warning status-color') {
                    input('4','type':'radio', 'id':'warning', 'name':'options', 'autocomplete':'off')
                }
                label('', 'class':attrs.value == 'danger' ? 'btn btn-danger status-color active' : 'btn btn-danger status-color') {
                    input('5','type':'radio', 'id':'danger', 'name':'options', 'autocomplete':'off')
                }
            }
        }

        // JavaScript Code
        sb.append("<script type='text/javascript'>").append("\$(function() {")
        sb.append("\$('.status-color').on('click', function() {")
        sb.append("\$('#${attrs.id}').val(\$(this).find('input').attr('id'));")
        sb.append("});});").append("</script>")

        sb.flush()

        out << sb.toString()
    }

    def auditedFields = { attrs ->
        if (attrs.bean == null) throw new IllegalArgumentException("Attribute 'bean' must be defined!")
        if (attrs.bean.id == null || request.xhr) {
            return;
        }

        final StringWriter sb = new StringWriter()
        def builder = new MarkupBuilder(sb)

        builder."div"('class': 'col-lg-12 box no-padding') {
            div('class': 'box-header no-padding') {
                div('class': 'box-name') {
                    i('', 'class': 'fa fa-info')
                    span(g.message(code: 'auditedData'))
                }
                div('class': 'box-icons') {
                    a('class': 'collapse-link'){
                        i('', 'class': 'fa fa-chevron-up')
                    }
                    a('class': 'close-link'){
                        i('', 'class': 'fa fa-times')
                    }
                }
            }
            div('class': 'box-content no-padding') {
                div('class': 'form-group col-lg-12') {
                    div('class': 'col-sm-2 control-label') {
                        label(g.message(code: 'createdBy'), 'for':'createdBy')
                    }
                    div('class': 'col-sm-4', 'style':'padding-top:4px;') {
                        span(g.fieldValue(bean:attrs.bean, field:'createdBy'), 'id':'createdBy')
                    }
                    div('class': 'col-sm-2 control-label') {
                        label(g.message(code: 'createdAt'), 'for':'createdAt')
                    }
                    div('class': 'col-sm-4', 'style':'padding-top:4px;') {
                        span(g.formatDate(date:attrs.bean.createdAt, format:'yyyy-MM-dd HH:mm:ss'), 'id':'createdAt')
                    }
                }
                div('class': 'form-group col-lg-12') {
                    div('class': 'col-sm-2 control-label') {
                        label(g.message(code: 'editedBy'), 'for':'editedBy')
                    }
                    div('class': 'col-sm-4', 'style':'padding-top:4px;') {
                        span(g.fieldValue(bean:attrs.bean, field:'editedBy'), 'id':'editedBy')
                    }
                    div('class': 'col-sm-2 control-label') {
                        label(g.message(code: 'editedAt'), 'for':'editedAt')
                    }
                    div('class': 'col-sm-4', 'style':'padding-top:4px;') {
                        span(g.formatDate(date:attrs.bean.editedAt, format:'yyyy-MM-dd HH:mm:ss'), 'id':'editedAt')
                    }
                }
            }
        }
        sb.flush()

        out << sb.toString()
    }

    def iconAuditedFields = { attrs ->
        if (attrs.bean == null) throw new IllegalArgumentException("Attribute 'bean' must be defined!")
        if (attrs.bean.id == null || request.xhr) {
            return;
        }

        final StringWriter sb = new StringWriter()
        def builder = new MarkupBuilder(new IndentPrinter(new PrintWriter(sb), "", false))

        builder.div('class': 'box-header no-padding') {
            div('class': 'box-name') {
                i('', 'class': 'fa fa-info')
                span(g.message(code: 'auditedData'))
            }
            div('class': 'box-icons') {
                a('class': 'close-link'){
                    i('', 'class': 'fa fa-times')
                }
            }
        }

        def header = sb.toString()
        sb.getBuffer().setLength(0)

        builder.div('class': 'box-content no-padding') {
            div('class': 'form-group col-lg-12') {
                div('class': 'col-sm-2 control-label', 'style':'padding-top:4px;') {
                    label(g.message(code: 'createdBy'), 'for':'createdBy')
                }
                div('class': 'col-sm-4', 'style':'padding-top:4px;') {
                    span(g.fieldValue(bean:attrs.bean, field:'createdBy'), 'id':'createdBy')
                }
                div('class': 'col-sm-2 control-label', 'style':'padding-top:4px;') {
                    label(g.message(code: 'createdAt'), 'for':'createdAt')
                }
                div('class': 'col-sm-4', 'style':'padding-top:4px;') {
                    span(g.formatDate(date:attrs.bean.createdAt, format:'yyyy-MM-dd HH:mm:ss'), 'id':'createdAt')
                }
            }
            div('class': 'form-group col-lg-12') {
                div('class': 'col-sm-2 control-label', 'style':'padding-top:4px;') {
                    label(g.message(code: 'editedBy'), 'for':'editedBy')
                }
                div('class': 'col-sm-4', 'style':'padding-top:4px;') {
                    span(g.fieldValue(bean:attrs.bean, field:'editedBy'), 'id':'editedBy')
                }
                div('class': 'col-sm-2 control-label', 'style':'padding-top:4px;') {
                    label(g.message(code: 'editedAt'), 'for':'editedAt')
                }
                div('class': 'col-sm-4', 'style':'padding-top:4px;') {
                    span(g.formatDate(date:attrs.bean.editedAt, format:'yyyy-MM-dd HH:mm:ss'), 'id':'editedAt')
                }
            }
        }

        def data = sb.toString()
        sb.getBuffer().setLength(0)

        builder.button('onclick': 'CloseModalBox();', 'type':'cancel', 'class':'save btn btn-primary pull-right'){
            span(g.message(code: 'default.button.close.label'))
        }

        def buttons = sb.toString()
        sb.getBuffer().setLength(0)

        out << '<a class="audit-data" style="margin-left: 5px;"><i class="fa fa-info-circle"></i></a>'
        out << '<script>\$(document).ready(function() {\$(\'.audit-data\').on(\'click\', function(){'
        out << 'var h = \$("' + header + '");'
        out << 'var d = \$("' + data + '");'
        out << 'var b = \$("' + buttons + '");'
        out << 'OpenModalBox(h, d, b);'
        out << '});});</script>'
    }

    def genericTableActions = { attrs, body ->
        if (attrs.instance == null) throw new IllegalArgumentException("Attribute 'instance' must be defined!")
        out << editLink(id: attrs.instance.id)
        out << "\n"
        out << deleteLink(id: attrs.instance.id, controller: attrs.controller)
    }

    def showLink = { attrs ->
        if (attrs.id == null)
            throw new IllegalArgumentException("Attribute 'id' must be defined!")

        out << createLink(g.createLink(action: 'show', id: attrs.id), 'btn btn-app-sms btn-success btn-table-header', 'fa fa-eye', null);
    }

    def editLink = { attrs ->
        if (attrs.id == null)
            throw new IllegalArgumentException("Attribute 'id' must be defined!")

        out << createLink(g.createLink(action: 'edit', id: attrs.id), 'btn btn-app-sms btn-info btn-table-header', 'fa fa-edit', null);
    }

    def deleteLink = { attrs ->
        if (attrs.id == null) throw new IllegalArgumentException("Attribute 'id' must be defined!", null)

        def linkAttr = [:]
        linkAttr['id'] = attrs.id
        if (attrs.deleteParams != null) linkAttr['params'] = attrs.deleteParams
        if (attrs.controller != null) linkAttr['controller'] = attrs.controller
        if (attrs.deleteAction != null) {
            linkAttr['action'] = attrs.deleteAction
        } else {
            linkAttr['action'] = "delete"
        }

        def link = g.createLink(linkAttr)

        def callback = (attrs.removeFromTableOnly != null && Boolean.valueOf(attrs.removeFromTableOnly)) ? REMOVE_FROM_TABLE_ONLY : DELETE_CALLBACK

        if (attrs.callback != null) {
            callback = attrs.callback
        }

        def removeFromTableOnly = (attrs.removeFromTableOnly != null && Boolean.valueOf(attrs.removeFromTableOnly))

        StringBuilder sb = new StringBuilder("var btn=\$(this);{noty({text:'")
                .append(g.message(code: 'generic.confirm'))
                .append("',type:'confirm',dismissQueue:false,layout:'center',theme:'defaultTheme',modal:true,buttons:[{addClass:'btn btn-primary',text:'")
                .append(g.message(code: 'default.button.confirm.positive'))
                .append("',onClick:function(\$noty){\$noty.close();\$.post('")
                .append(link)
                .append("').done(function(data){noty({text:data,layout:'top',timeout:5000,modal:false,buttons:false});")
                .append(callback).append("});}},{addClass:'btn btn-danger',text:'")
                .append(g.message(code: 'default.button.confirm.negative'))
                .append("',onClick:function(\$noty){\$noty.close();}}]});}")


        out << createLink('', 'btn btn-sm btn-danger btn-table-header', 'fa fa-trash', sb.toString());
    }

    def deleteConfirmationLink = { attrs ->
        if (attrs.id == null) throw new IllegalArgumentException("Attribute 'id' must be defined!", null)

        def linkAttr = [:]
        linkAttr['id'] = attrs.id
        if (attrs.deleteParams != null) linkAttr['params'] = attrs.deleteParams
        if (attrs.controller != null) linkAttr['controller'] = attrs.controller
        if (attrs.deleteAction != null) {
            linkAttr['action'] = attrs.deleteAction
        } else {
            linkAttr['action'] = "delete"
        }

        def deleteLink =  g.createLink(linkAttr)

        StringBuilder sb = new StringBuilder("<a type=\"button\" role=\"button\" class=\"btn btn-outline\" href=\"deleteConfirmationLink")
        sb.append(deleteLink)
        sb.append("\">")
        sb.append(g.message(code: 'default.button.confirm.positive'))
        sb.append("<a/>")
        out << sb.toString()
    }

    /**
     * Creates link to ajax call for the form to create bean
     *
     * @usages:
     * - Minimum setup: controller
     * - Available attributes:
     * 1) String controller (required) - controller with ajax action that return form
     * 2) String id (optional) - DOM identifier of element
     * 3) String action (optional) - action that return form, default is ajaxCreate
     * 4) String saveAction (optional) - action that saves form, default is ajaxSave
     * 5) String iconClass (optional) - CSS class added to icon element, default is fa fa-plus
     * 6) String params (optional) - additional params passed to get form action
     * 7) String class (optional) - CSS class added to link element
     */
    def createAjaxLink = { attrs, body ->
        if (attrs.controller == null)
            throw new IllegalArgumentException("Attribute 'controller' must be defined!", null)

        if (attrs.id == null) attrs.id = "create_" + attrs.hashCode()
        if (attrs.action == null) attrs.action = "ajaxCreate"
        if (attrs.saveAction == null) attrs.saveAction = "ajaxSave"
        if (attrs.saveParams == null) attrs.saveParams = []
        if (attrs.iconClass == null) attrs.iconClass = "fa fa-plus"

        def linkAttr = [controller: attrs.controller, action: attrs.action]

        if (attrs.params != null) {
            linkAttr['params'] = attrs.params
        }

        def link = g.createLink(linkAttr)

        linkAttr = ['class': attrs.class, 'href' : link, id: attrs.id]

        final StringWriter sb = new StringWriter()
        def builder = new MarkupBuilder(sb)

        builder."a"(linkAttr, body().toString()) {
            span('') {
                i('', 'class': attrs.iconClass)
            }
        }

        def saveLink = g.createLink(controller: attrs.controller, action: attrs.saveAction, params: attrs.saveParams)


        // JavaScript Code
        sb.append("<script>").append("\$(document).ready(function() {")
        sb.append(createAddAjaxForm(attrs.id, attrs.controller, link, null, saveLink, attrs.controller, null, false, true));
        sb.append("});").append("</script>")

        sb.flush()

        out << sb.toString()
    }

    /**
     * Creates button with link to ajax call for the form to create bean
     *
     * @usages:
     * - Minimum setup: controller
     * - Available attributes:
     * 1) String controller (required) - controller with ajax action that return form
     * 2) String id (optional) - DOM identifier of element
     * 3) String action (optional) - action that return form, default is ajaxCreate
     * 4) String saveAction (optional) - action that saves form, default is ajaxSave
     * 5) String iconClass (optional) - CSS class added to icon element, default is fa fa-plus
     * 6) String params (optional) - additional params passed to get form action
     * 7) String class (optional) - CSS class added to link element
     */
    def ajaxCreateButton = { attrs, body ->

        if (attrs.controller == null)
            throw new IllegalArgumentException("Attribute 'controller' must be defined!", null)

        if (attrs.id == null) attrs.id = "create_" + attrs.hashCode()
        if (attrs.action == null) attrs.action = "ajaxCreate"
        if (attrs.saveAction == null) attrs.saveAction = "ajaxSave"
        if (attrs.saveParams == null) attrs.saveParams = []
        if (attrs.iconClass == null) attrs.iconClass = "fa fa-plus"

        def linkAttr = [controller: attrs.controller, action: attrs.action]

        def toggleAppend = ""
        if (attrs.'data-toggle' && attrs.'data-toggle' == "tooltip")  {
            toggleAppend = " data-toggle='tooltip' data-placement='${attrs.'data-placement' == null ? 'top' : attrs.'data-placement'}' data-title='${attrs.title == null ? '' : attrs.title}' "
        }
        def titleAttr = ""
        if (attrs.title)  {
            titleAttr = " title='${attrs.title}' "
        }

        if (attrs.params != null) {
            linkAttr['params'] = attrs.params
        }

        def createLink = g.createLink(linkAttr)

        def dialogTitle = g.message(code: 'default.button.create.label') + "&hellip;"

        StringBuilder sb = new StringBuilder()
        sb.append("<button type='button' ")
            .append("id='${attrs.id}' ")
            .append(toggleAppend)
            .append(titleAttr)
            .append("class='${attrs.class}'>")
            .append(" <span class='${attrs.iconClass}'></span>")
            .append("</button>")

        def saveLink = g.createLink(controller: attrs.controller, action: attrs.saveAction, params: attrs.saveParams)


        // JavaScript Code

        sb.append("<script>")
        sb.append(buildAjaxForm(attrs.id, attrs.controller, createLink, null, saveLink, dialogTitle))
        sb.append("</script>")

        out << sb.toString()
    }

    /**
     * Creates button with link to ajax call for the form to create bean
     *
     * @usages:
     * - Minimum setup: controller
     * - Available attributes:
     * 1) String controller (required) - controller with ajax action that return form
     * 2) String id (optional) - DOM identifier of element
     * 3) String action (optional) - action that return form, default is ajaxCreate
     * 4) String saveAction (optional) - action that saves form, default is ajaxSave
     * 5) String iconClass (optional) - CSS class added to icon element, default is fa fa-plus
     * 6) String params (optional) - additional params passed to get form action
     * 7) String class (optional) - CSS class added to link element
     */
    def ajaxEditButton = { attrs, body ->

        if (attrs.controller == null)
            throw new IllegalArgumentException("Attribute 'controller' must be defined!", null)

        if (attrs.id == null) attrs.id = "create_" + attrs.hashCode()
        if (attrs.action == null) attrs.action = "ajaxEdit"
        if (attrs.saveAction == null) attrs.saveAction = "ajaxSave"
        if (attrs.saveParams == null) attrs.saveParams = []
        if (attrs.iconClass == null) attrs.iconClass = "fa fa-eye"

        def linkAttr = [controller: attrs.controller, action: attrs.action, id: attrs.objectId]

        def toggleAppend = ""
        if (attrs.'data-toggle' && attrs.'data-toggle' == "tooltip")  {
            toggleAppend = " data-toggle='tooltip' data-placement='${attrs.'data-placement' == null ? 'top' : attrs.'data-placement'}' data-title='${attrs.title == null ? '' : attrs.title}' "
        }
        def titleAttr = ""
        if (attrs.title)  {
            titleAttr = " title='${attrs.title}' "
        }

        if (attrs.params != null) {
            linkAttr['params'] = attrs.params
        }

        def createLink = g.createLink(linkAttr)

        def dialogTitle = g.message(code: 'default.button.update.label') + "&hellip;"

        StringBuilder sb = new StringBuilder()
        sb.append("<button type='button' ")
                .append("id='${attrs.id}' ")
                .append(toggleAppend)
                .append(titleAttr)
                .append("class='${attrs.class}'>")
                .append(" <span class='${attrs.iconClass}'></span>")
                .append("</button>")

        def saveLink = g.createLink(controller: attrs.controller, action: attrs.saveAction, id: attrs.objectId)


        // JavaScript Code

        sb.append("<script>")
        sb.append(buildAjaxForm(attrs.id, attrs.controller, createLink, null, saveLink, dialogTitle))
        sb.append("</script>")

        out << sb.toString()
    }

    /**
     * Creates link to ajax call for the form to edit bean
     *
     * @usages:
     * - Minimum setup: controller, objectId
     * - Available attributes:
     * 1) String controller (required) - controller with ajax action that return form
     * 2) String objectId (required) - edited object id
     * 3) String id (optional) - DOM identifier of element
     * 4) String action (optional) - action that return form, default is ajaxEdit
     * 5) String saveAction (optional) - action that saves form, default is ajaxSave
     * 6) String iconClass (optional) - CSS class added to icon element, default is fa fa-eye
     * 7) String params (optional) - additional params passed to get form action
     * 8) String class (optional) - CSS class added to link element
     */
    def editAjaxLink = { attrs, body ->
        if (attrs.controller == null)
            throw new IllegalArgumentException("Attribute 'controller' must be defined!", null)
        if (attrs.objectId == null)
            throw new IllegalArgumentException("Attribute 'objectId' must be defined!", null)

        if (attrs.id == null) attrs.id = "edit_" + attrs.hashCode()
        if (attrs.action == null) attrs.action = "ajaxEdit"
        if (attrs.saveAction == null) attrs.saveAction = "ajaxSave"
        if (attrs.iconClass == null) attrs.iconClass = "fa fa-eye"

        def linkAttr = [controller: attrs.controller, action: attrs.action, id: attrs.objectId]

        if (attrs.params != null) {
            linkAttr['params'] = attrs.params
        }

        def link = g.createLink(linkAttr)

        linkAttr = ['class': attrs.class, 'href' : link, id: attrs.id]

        final StringWriter sb = new StringWriter()
        def builder = new MarkupBuilder(sb)

        builder."a"(linkAttr, body().toString()) {
            span('') {
                i('', 'class': attrs.iconClass)
            }
        }

        def saveLink = g.createLink(controller: attrs.controller, action: attrs.saveAction, id: attrs.objectId)

        def saveAccess = true
        if (attrs.saveAccess != null) {
            origAttrsMinusCustom.remove('saveAccess')
            saveAccess = SpringSecurityUtils.ifAnyGranted(attrs.saveAccess)
        }

        // JavaScript Code
        sb.append("<script>").append("\$(document).ready(function() {")
        sb.append(createAddAjaxForm(attrs.id, attrs.controller, link, null, saveLink, attrs.controller, null, false, saveAccess));
        sb.append("});").append("</script>")

        sb.flush()

        out << sb.toString()
    }

    def orgUnitTree = { attrs, body ->
        final StringWriter sb = new StringWriter()
        sb.append("<div class='tree'><ul>")
        def childUnits = OrgUnit.findAllByParentUnitIsNull()
        if(!childUnits.isEmpty()) {
            for (OrgUnit child: childUnits) {
                sb.append(buildSubTree(child))
            }
            sb.append("</ul></li>")
        } else {
            sb.append("<li><div>EMPTY ORGANIZATION</div></li>")
        }
        sb.append("</ul></div>")
        out << sb.toString()
    }

    def buildSubTree(OrgUnit bean) {
        final StringWriter sb = new StringWriter()
        def childUnits = OrgUnit.findAllByParentUnit(bean,[sort: "parentUnit", order: "desc"])
        if(!childUnits.isEmpty()) {
            sb.append("<li><div><p><strong>${bean.name}</strong></p>")
            sb.append("<a id='add_orgUnit_${bean.id}' class='create btn btn-primary btn-table-header'><span class='fa fa-plus'></span></a>" +
                    "<a id='show_orgUnit_${bean.id}' class='btn btn-app-sms btn-info btn-table-header'><span class='fa fa-edit'></span></a>" +
                    deleteLink(id: bean.id, controller: 'orgUnit') +
                    "</div><ul>")

            sb.append("<script>").append("\$(document).ready(function() {")
            if (!request.xhr) {

                def buttonId = "add_orgUnit_${bean.id}"
                def saveLink = g.createLink(controller: 'orgUnit', action: 'ajaxSave')
                def link = g.createLink(controller: 'orgUnit', action: 'ajaxCreateChild',id: bean.id)
                sb.append(createAddAjaxForm("show_orgUnit_${bean.id}", 'orgUnit', g.createLink(controller: 'orgUnit', action: 'ajaxEdit'), "${bean.id}", saveLink, 'orgUnit', 'orgUnit', false, true));
                def createAddAjaxFormS = createAddAjaxForm(buttonId, 'orgUnit', link, null, saveLink, 'orgUnit', 'orgUnit', false, true)
                log.debug(createAddAjaxFormS)
                sb.append(createAddAjaxFormS);
            }
            sb.append("});").append("</script>")
            for (OrgUnit child: childUnits) {
                sb.append(buildSubTree(child))
            }
            sb.append("</ul></li>")
        } else {
            sb.append(buildTreeItem(bean)).append("</li>")
        }
        return sb.toString()
    }

    def buildTreeItem(OrgUnit bean) {
        final StringWriter sb = new StringWriter()
        sb.append("<li><div><p><strong>${bean.name}</strong></p>")
        sb.append("<a id='add_orgUnit_${bean.id}' class='create btn btn-primary btn-table-header'><span class='fa fa-plus'></span></a>" +
                "<a id='show_orgUnit_${bean.id}' class='btn btn-app-sms btn-info btn-table-header'><span class='fa fa-edit'></span></a>" +
                deleteLink(id: bean.id, controller: 'orgUnit') +
                "</div>")

        sb.append("<script>").append("\$(document).ready(function() {")
        if (!request.xhr) {

            def buttonId = "add_orgUnit_${bean.id}"
            def saveLink = g.createLink(controller: 'orgUnit', action: 'ajaxSave')
            def link = g.createLink(controller: 'orgUnit', action: 'ajaxCreateChild',id: bean.id)
            sb.append(createAddAjaxForm("show_orgUnit_${bean.id}", 'orgUnit', g.createLink(controller: 'orgUnit', action: 'ajaxEdit'), "${bean.id}", saveLink, 'orgUnit', 'orgUnit', false, true));
            def createAddAjaxFormS = createAddAjaxForm(buttonId, 'orgUnit', link, null, saveLink, 'orgUnit', 'orgUnit', false, true)
            log.debug(createAddAjaxFormS)
            sb.append(createAddAjaxFormS);
        }
        sb.append("});").append("</script>")
        return sb.toString()
    }

    def attachments = { attrs, body ->
        def bean = attrs.remove('bean')

        if (bean?.metaClass?.hasProperty(bean, 'attachments') != null) {
            def inputName = attrs.remove('inputName') ?: 'attachment'
            def labelCode =  attrs.remove('labelCode') ?: 'attachments.label'
            def wrapper = inputName + 'Wrapper'
            // html
            final StringBuilder sb = new StringBuilder()
            sb.append("<div id='$wrapper'>")
            bean?.getAttachments(inputName)?.each { attachment ->
                def attachmentLink = g.createLink(controller: 'attachmentable', action: 'show', params:['id':attachment.id])
                sb.append("<div id='$inputName" + "_$attachment.id' class='col-lg-6 col-md-12 col-xs-12 col-sm-12'>")
                        .append("<iframe id='iframepdf_$attachment.id'  style='width: 100%; height:425px;' frameborder='0' src='${attachmentLink}'></iframe>")
                        .append(deleteLink(id: attachment.id, controller: 'attachmentable', callback: "\$('#$inputName" + "_$attachment.id').remove();"))
                        .append(createLink(g.createLink(action: 'download', controller: 'attachmentable', id: attachment.id), 'btn btn-app-sms btn-success pull-right', 'fa fa-download', null))
                        .append("</div>")
            }
            sb.append("</div>")
            def uploadLink = g.createLink(controller: 'attachmentable', action: 'upload')
            def addMsg = g.message(code: 'attachment.upload.add.button.name')
            sb.append("<div class='form-group col-lg-12'>")
                    .append("<form id='$inputName" + "UploadForm' class='MultiFile-intercepted' enctype='multipart/form-data' name='$inputName" + "UploadForm' method='post' action='$uploadLink'>")
                    .append("<input id='referenceClass' type='hidden' value='${bean.class.name}' name='attachmentLink.referenceClass'>")
                    .append("<input id='referenceId' type='hidden' value='${bean.id}' name='attachmentLink.referenceId'>")
                    .append("<input id='attachmentType' type='hidden' value='${inputName}' name='attachmentType'>")
                    .append("<div id='attachmentWrap' class='MultiFileWrap'>")
                    .append("<span class='btn btn-success fileinput-button'><i class='fa fa-plus'></i><span>${addMsg}</span><input type='file' name='$inputName' id='$inputName' file_extension='.pdf' multiple></span>")
                    .append("</div>").append("</form>").append("</div>")

            //javascript
            def showLink = g.createLink(controller: 'attachmentable', action: 'show')
            def downloadLink = g.createLink(controller: 'attachmentable', action: 'download')
            def deleteLink = g.createLink(controller: 'attachmentable', action: 'delete')
            sb.append("<script>").append("\$(document).ready(function(){")
                    .append("\$('#$inputName').on('change',function(){\$('#$inputName" + "UploadForm').submit();});")
                    .append("var form = \$('#$inputName" + "UploadForm').ajaxForm({")
                    .append("success: function(responseText, statusText){\$('#attachment').val('');")
                    .append("for (i = 0; i < responseText.length; i++) {")
                    .append("if (!\$('#$inputName" + "_' + responseText[i]).length > 0) {")
                    .append("var attachmentDiv = \$('<div id=\"$inputName" + "_' + responseText[i] + '\" class=\"col-lg-6 col-md-12 col-xs-12 col-sm-12\"><iframe id=\"iframepdf_' + responseText[i] + '\" frameborder=\"0\" src=\"$showLink/' + responseText[i] + '\" style=\"width: 100%; height:425px;\"></iframe></div>');")
                    .append("var downloadLink = \$('<a href=\"$downloadLink/' + responseText[i] + '\" class=\"btn btn-app-sms btn-success pull-right\"><span class=\"fa fa-download\"></span></a>');")
                    .append("var deleteLink = \$('<a class=\"btn btn-app-sms btn-danger pull-left\" onclick=\"var btn=\$(this);{noty({text:\\'Are You sure?\\',type:\\'confirm\\',dismissQueue:false,layout:\\'center\\',theme:\\'defaultTheme\\',modal:true,buttons:[{addClass:\\'btn btn-primary\\',text:\\'Yes\\',onClick:function(\$noty){\$noty.close();\$.post(\\'$deleteLink/' + responseText[i] + '\\').done(function(data){noty({text:data,layout:\\'top\\',timeout:5000,modal:false,buttons:false});\$(\\'#$inputName" + "_'+ responseText[i] + '\\').remove();});}},{addClass:\\'btn btn-danger\\',text:\\'Cancel\\',onClick:function(\$noty){\$noty.close();}}]});}\"><span class=\"fa fa-trash\"></span></a>');")
                    .append("\$(attachmentDiv).append(deleteLink).append(downloadLink);")
                    .append("\$('#$wrapper').append(attachmentDiv);")
                    .append("}}}, error: function(data){noty({text:data.responseText,type:'error',layout:'top',timeout:5000,modal:false,buttons:false});}")
                    .append("});").append("});").append("</script>")
            out << sb.toString()
        }
    }

    def findAndAddManyToMany = { attrs, body ->
        if (attrs.controller == null) throw new IllegalArgumentException("Attribute 'controller' must be defined!", null)
        if (attrs.id == null) attrs.id = "autocomplete_" + attrs.hashCode()
        if (request.xhr) attrs.id += "_xhr"
        def error = StringUtils.isNotBlank(attrs.error) ? attrs.error : '';
        attrs.remove('error')
        def multiple = attrs.multiple != null ? Boolean.valueOf(attrs.multiple) : false
        def pageLimit = attrs.pageLimit != null ? Integer.valueOf(attrs.pageLimit) : 10

        // html
        final StringWriter sb = new StringWriter()
        def builder = new MarkupBuilder(sb)

        builder."div"('class': error) {
            div('class': 'col-sm-3 control-label') {
                def labelCode = attrs.labelCode != null ? attrs.labelCode : 'label'
                def labelMsg = g.message(code: labelCode)
                label('for': attrs.id, labelMsg)
            }
            div('class': 'col-sm-9') {
                input('', 'type': 'text', id: attrs.id, value: attrs.value, style: 'width: 89%')
                if (attrs.addAction != null) {
                    a(class: 'create btn btn-primary add-inline', id:"add_$attrs.id") {
                        span('', class: 'fa fa-plus')
                    }
                }
            }
        }

        // javascript code
        def linkAttr = [:]
        linkAttr['controller'] = attrs.controller
        linkAttr['action'] = attrs.autocompleteAction != null ? attrs.autocompleteAction : 'autocomplete'

        def autocompleteLink = g.createLink(linkAttr)

        linkAttr = [:]
        linkAttr['controller'] = attrs.controller
        linkAttr['action'] = attrs.selectionAction != null ? attrs.selectionAction : 'findById'

        def selectionLink = g.createLink(linkAttr)

        def locale = RequestContextUtils.getLocale(request).toString().substring(0, 2)
        sb.append("<script>").append("\$(document).ready(function() {")
                .append("\$('#$attrs.id').select2({")
                .append("initSelection:function(element,callback) {if(\$('#$attrs.id').val()!==''){\$.getJSON('$selectionLink/'+\$('#$attrs.id').val(),function(data){callback(data);});}},")
                .append("language:'$locale',allowClear:true,minimumInputLength:3,multiple:$multiple,")
                .append("placeholder:'").append(g.message(code: attrs.controller)).append("',")
                .append("closeOnSelect:true,theme:'bootstrap',")
                .append("ajax:{cache:false,type:'GET',dataType:'JSON',url:'$autocompleteLink',quietMillis:500,data:function(term,page){return {term:term,offset:page-1,limit:$pageLimit};},results:function(data,page){return {results:data.items,more:(page*$pageLimit)<data.total};}}")
                .append("});")

        if (attrs.addAction != null) {
            sb.append("\$('#$attrs.id').on('change', function(event) {")
                    .append("if(\$(this).val() == ''){\$('#add_$attrs.id').addClass('disabled');}else{\$('#add_$attrs.id').removeClass('disabled');}")
                    .append("});")
                    .append("if(\$('#$attrs.id').val() == ''){\$('#add_$attrs.id').addClass('disabled');}else{\$('#add_$attrs.id').removeClass('disabled');}")

                    .append("\$('#add_$attrs.id').on('click',function(){\$.ajax({type:'POST',dataType:'JSON',")

            if (attrs.addAddtionalParamName != null && attrs.addAddtionalParamValue) {
                sb.append("data: {$attrs.addAddtionalParamName: $attrs.addAddtionalParamValue},")
            }

            linkAttr = [:]
            linkAttr['controller'] = attrs.controller
            linkAttr['action'] = attrs.addAction
            def addLink = g.createLink(linkAttr)

            sb.append("url:'$addLink/'+\$('#$attrs.id').val(),cache: false")
                    .append("}).done(function(data) {")
                    .append("noty({text:data.message,type:'success',layout:'top',timeout:5000,modal:false,buttons:false});")
                    .append("if (data.success === true){\$('#$attrs.id').val('');setTimeout(function(){location.reload();},1500);}")
                    .append("}).fail(function() {")
                    .append("noty({text:\"").append(g.message(code: 'error.generic')).append("\",type:'error',layout:'top',timeout:5000,modal:false,buttons:false});")
                    .append("});").append("});")
        }

        sb.append("});").append("</script>")

        sb.flush()

        out << sb.toString()
    }

    private String createLink(final String href, final String linkClass, final String spanClass, final String onClickCallback) {
        final StringWriter writer = new StringWriter()
        def builder = new MarkupBuilder(writer)
        def linkAttr = new HashMap('class': linkClass)
        if (StringUtils.isNotBlank(href)) {
            linkAttr['href'] = href
        }
        if (onClickCallback) linkAttr['onclick'] = onClickCallback

        builder."a"(linkAttr) {
            span('', 'class': spanClass)
        }

        writer.flush()
        return writer.toString()
    }

    private String buildAjaxForm(final String buttonId, final String controller, final String createLink, final String additionalCreateLinkAttr, final String saveLink, final String dialogTitleCode) {
        final StringBuilder sb = new StringBuilder()
        sb.append("\$('#$buttonId').on('click', function(event) {")
            .append("var html_object = '';")
            .append("\$.ajax({")
            .append("type: 'POST',")
            .append("url: '${createLink}")
            .append(StringUtils.isNotBlank(additionalCreateLinkAttr) ? "/${additionalCreateLinkAttr}'" : "'").append(",")
            .append("success: function(model) {")
            .append("html_object = \"<form id='${controller}Form' role='form'>\";")
            .append("html_object = html_object +  model;")
            .append("html_object = html_object + \"</form>\";")
            .append("console.log(html_object);")
            .append("var dialog = bootbox.dialog({")
            .append("title: '").append(g.message(code: dialogTitleCode)).append("',")
            .append("message: html_object,")
            .append("size: 'large',")
            .append("buttons: {")
            .append("cancel: {")
            .append("label: '<i class=\"fa fa-times\"></i> ").append(g.message(code: 'default.button.cancel.label')).append("',")
            .append("className: 'btn-danger pull-left',")
            .append("callback: function(){console.log('Custom cancel clicked');}")
            .append("},")
            .append("ok: {")
            .append("label: '<i class=\"fa fa-check\"></i> ").append(g.message(code: 'default.button.save.label')).append("',")
            .append("className: 'btn-info pull-right',")
            .append("callback: function(){")
            .append("console.log('Custom OK clicked');")
            .append("\$.ajax({")
            .append("type: 'POST',")
            .append("url: '${saveLink}',")
            .append("data: \$('#${controller}Form').serialize(),")
            .append("success: function(saveModel) {location.reload();}")
            .append("});")
            .append("}")
            .append("}")
            .append("}")
            .append("});")
            .append("\$('.bootbox-body .datepicker').datepicker({language:'pl',format:'yyyy-mm-dd',todayHighlight: true,autoclose: true});")
            .append("}")
            .append("});")
            .append("});")
        return sb.toString()
    }


    private String createAddAjaxForm(final String buttonId, final String controller, final String formLink, final String additionalFormLinkAttr, final String saveLink, final String dialogTitleCode, final String htmlObjectId, final boolean autocomplete, final boolean saveAccess) {
        def formId = controller + "Form"
        def cancelCssClass = "btn btn-danger btn-label-left"
        def closeMessageId = "default.button.cancel.label"
        if (!saveAccess) {
            cancelCssClass = "btn btn-primary btn-label-left pull-right"
            closeMessageId = "default.button.close.label"
        }
        final StringBuilder sb = new StringBuilder()
        sb.append("\$('#$buttonId').on('click', function(event) {")
                .append("event.preventDefault();")
                .append("var form = \$('<form id=\"$formId\">');")
                .append("fieldsetItem = \$('<fieldset class=\"buttons\">');")
                .append("cancelButton = document.createElement('button');")
                .append("\$(cancelButton).attr({id:'event_cancel',type: 'cancel',class: '$cancelCssClass',onclick: 'CloseModalBox();'}).text('")
                .append(g.message(code: closeMessageId)).append("');")

        if (saveAccess) {
            sb.append("submitButton = document.createElement('button');")
                    .append("\$(submitButton).attr({id:'event_submit',class: 'save btn btn-primary btn-label-left pull-right',")
                    .append("href:'").append(saveLink).append("',")
                    .append("onclick: \"jQuery.ajax({type:'POST',data:jQuery('#").append(controller).append("Form').serialize(), url:")
                    .append("'$saveLink'")
            if (StringUtils.isNotBlank(additionalFormLinkAttr)) {
                sb.append("+'/'+$additionalFormLinkAttr")
            }
            sb.append(",success:function(data,textStatus){")
                    .append("if(data.success===true){var modalbox = \$('#modalbox');modalbox.hide();modalbox.find('.modal-header-name span').children().remove();modalbox.find('.devoops-modal-inner').children().remove();modalbox.find('.devoops-modal-bottom').children().remove();\$('body').removeClass('body-expanded');noty({text:data.message,type:'success',layout:'top',timeout:5000,modal:false,buttons:false});")
            if (htmlObjectId) {
                if (!autocomplete) {
                    sb.append("\$('#$htmlObjectId').select2('destroy');\$('#$htmlObjectId option[value='+data.object.id+']').remove();\$('#$htmlObjectId').append(\$('<option>',{value :data.object.id}).text(data.object.name));\$('#$htmlObjectId').select2();\$('#$htmlObjectId').select2('val',data.object.id);if(\$('#$htmlObjectId').prop('oninput')!==null){\$('#$htmlObjectId')[0].oninput();}")
                } else {
                    sb.append("\$('#$htmlObjectId').val(data.object.id).trigger('change');if(\$('#$htmlObjectId').prop('oninput')!==null){\$('#$htmlObjectId')[0].oninput();}")
                }
            } else {
                sb.append("setTimeout(function(){location.reload();},1500);")
            }
            sb.append("}else{var mainDiv = \$('<div>');mainDiv.addClass('col-lg-12 alert alert-block alert-danger');mainDiv.css('text-align','center');var header = \$('<p>');header.addClass('col-lg-12');var headerText = \$('<h4>');headerText.css('font-weight','bold');")
                    .append("headerText.text('").append(g.message(code: 'bean.errors')).append("');header.append(headerText);var closeLink=\$('<a>');closeLink.addClass('close');closeLink.attr('data-dismiss','alert');closeLink.text('×');mainDiv.append(header);mainDiv.append(closeLink);\$.each(data.errors,function(index,value){var pElement = \$('<p>');pElement.html(value);mainDiv.append(pElement);});\$('.devoops-modal-inner').prepend(mainDiv);}")
                    .append("},error:function(XMLHttpRequest,textStatus,errorThrown){noty({text:'").append(g.message(code: 'error.generic')).append("',layout:'top',type:'error',timeout:5000,modal:false,buttons:false});}});return false\"}).text('")
                    .append(g.message(code: 'default.button.save.label')).append("');")
        }


        sb.append("\$(fieldsetItem).append(cancelButton);")
        if (saveAccess) {
            sb.append("\$(fieldsetItem).append(submitButton);")
        }
        sb.append("form.load('$formLink'")

        if (StringUtils.isNotBlank(additionalFormLinkAttr)) {
            sb.append("+'/'+$additionalFormLinkAttr")
        }
        if (!saveAccess) {
            sb.append(", function() {\$('#$formId :input').attr('disabled', true);}")
        }

        sb.append(");")
                .append("OpenModalBox(\"").append(g.message(code: dialogTitleCode)).append("\", form, fieldsetItem);")

        sb.append("});")

        return sb.toString();
    }
}