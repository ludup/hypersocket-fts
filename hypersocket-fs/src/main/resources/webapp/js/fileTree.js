$.fn.fileTree = function(data) {
	var id = $(this).attr('id');
	var include = function(node){
		if(node.id == '#' || node.text == '/'){
			for(var index = 0; index < node.children.length; index++){
				include($('#' + id + 'TreeView').jstree().get_node(node.children[index]));
			}
		}else{
			
			var fullPath = node.text;
			for(var index = 0; index < node.parents.length - 2; index++){
				fullPath = $('#' + id + 'TreeView').jstree().get_node(node.parents[index]).text + '/' + fullPath;
			}
			addFullPath('/' + fullPath);
		}
	}
	
	var addFullPath = function(fullPath){
		var found = false;
		$('#' + id + 'Included option[value^="' + fullPath + '"]').remove();
		$('#' + id + 'Included option').each(function(index, option){
			
			if(fullPath.startsWith($(option).attr('value'))){
				found = true;
				return false;
			}else if($(option).attr('value') > fullPath){
				$(option).before('<option value="' + fullPath + '">' + fullPath + '</option>');
				found = true;
				return false;
			}
		});
		if(!found){
			$('#' + id + 'Included').append('<option value="' + fullPath + '">' + fullPath + '</option>');
		}
	}

	if ($(this).data('created')) {
		options = $(this).widget().options();
		
		return;

	} else {

		var options = $.extend(
		{	valueAttr : 'id', 
			nameAttr : 'name', 
			nameIsResourceKey : false, 
			disabled : false,
			fileTreeResourceKey: 'text.fileTree',
			includedLabelResourceKey: 'text.included',
			getUrlData: function(data) {
				return data;
			}
		}, data);


		var callback = {
				setValue: function(val) {
					if(Object.prototype.toString.call(val) === '[object Array]'){
						$.each(val, function(index, fullPath){
							addFullPath(fullPath);
						});
					}else if(typeof val === 'string'){
						addFullPath(fullPath);
					}
					if(options.changed) {
			 			options.changed(callback);
			 		}
				},
				getValue: function() {
					var values = '';
					$('#' + id + 'Included option').each(function(index, option){
						if(values != ''){
							values = values + ']|[';
						}
						values = values + $(option).val(); 
					});
					return values;
				},
				reset: function() {
					
				},
				disable: function() {
					options.disabled = true;
					$('#' + id + 'Included').attr('disabled', true);
					$('#' + id + 'Include').attr('disabled', 'disabled');
					$('#' + id + 'IncludeAll').attr('disabled', 'disabled');
					$('#' + id + 'Exclude').attr('disabled', 'disabled');
					$('#' + id + 'ExcludeAll').attr('disabled', 'disabled');
				},
				enable: function() {
					options.disabled = false;
					$('#' + id + 'Included').attr('disabled', false);
					$('#' + id + 'Include').removeAttr('disabled');
					$('#' + id + 'IncludeAll').removeAttr('disabled');
					$('#' + id + 'Exclude').removeAttr('disabled');
					$('#' + id + 'ExcludeAll').removeAttr('disabled');
				},
				options: function() {
					return options;
				},
				getInput: function() {
					return $('#' + id);
				},
	 			clear: function() {
	 				$('#' + id + 'Included option').remove();
	 			}
		};
		
		$('#' + id).append(
					'<div class="row" style="margin-bottom:25px;">'
				+	'	<div style="height:400px;" class="col-md-6">'
				+	'		<label>' + getResource(options.fileTreeResourceKey) + '</label>'
				+	'		<div class="panel panel-body" id="' + id + 'TreeView" style="height:100%; overflow: auto;"></div>'
				+	'	</div>'
				+	'	<div class="col-md-1" style="padding-top:140px; padding-left:0px; padding-right:0px;">'
				+	'		<button id="' + id + 'Include" class="btn btn-small btn-primary" style="margin-bottom:10px; width:100%;"><i class="fa fa-chevron-right" style="margin:0px;"></i></button>'
				+	'		<button id="' + id + 'IncludeAll" class="btn btn-small btn-primary" style="margin-bottom:10px; width:100%;"><i class="fa fa-chevron-right" style="margin:0px;"></i><i class="fa fa-chevron-right" style="margin:0px;"></i></button>'
				+	'		<button id="' + id + 'Exclude" class="btn btn-small btn-primary" style="margin-bottom:10px; width:100%;"><i class="fa fa-chevron-left" style="margin:0px;"></i></button>'
				+	'		<button id="' + id + 'ExcludeAll" class="btn btn-small btn-primary" style="margin-bottom:10px; width:100%;"><i class="fa fa-chevron-left" style="margin:0px;"></i><i class="fa fa-chevron-left" style="margin:0px;"></i></button>'
				+	'	</div>'
				+	'	<div style="height:400px;" class="col-md-5">'
				+	'		<label>' + getResource(options.includedLabelResourceKey) + '</label>'
				+	'		<select id="' + id + 'Included" multiple="multiple" class="panel-body formInput text form-control" style="height:100%;padding:5px;">'
				+	'		</select>'
				+	'	</div>'
				+	'</div>');
		
		if(options.values && options.values.length){
			$.each(options.values, function(index, value){
				$('#' + id + 'Included').append('<option value="' + value + '">' + value + '</option>');
			});
			
			if(options.changed) {
	 			options.changed(callback);
	 		}
		}
		
		$('#' + id + 'Include').click(function(){
			if(!options.disabled){
				var selected = $('#' + id + 'TreeView').jstree('get_selected', true);
				$.each(selected, function(index, node){
					include(node);
				});
				if(selected && selected.length > 0 && options.changed) {
			 		options.changed(callback);
				}
			}
		});
		
		$('#' + id + 'IncludeAll').click(function(){
			if(!options.disabled){
				include($('#' + id + 'TreeView').jstree().get_node('#'));
			}
		});
		
		$('#' + id + 'Exclude').click(function(){
			if(!options.disabled){
				var selected = $('#' + id + 'Included').val();
				var changed = false;
				if(selected && selected.length){
					$('#' + id + 'Included option').each(function(index, option){
						if(selected.indexOf($(option).val()) != -1){
							$(option).remove();
							changed = true;
						}
					});
				}
				if(changed && options.changed) {
		 			options.changed(callback);
		 		}
			}
		});
		
		$('#' + id + 'ExcludeAll').click(function(){
			if(!options.disabled){
				var numIncluded = $('#' + id + 'Included option').length;
				$('#' + id + 'Included option').remove();
				if(numIncluded > 0 && options.changed){
					options.changed(callback);
				}
			}
		});
		
		var name = (options && options.resourceKey != null ) ? formatResourceKey(options.resourceKey) : id ;
		$('#' + id + 'TreeView').jstree({
			"core" : {
				"animation" : 0,
			    "multiple" : true,
			    "themes" : { "stripes" : false },
			    "data": {
			    	"url" : function(node){
			    		var url = basePath + '/api/' + options.url;
		    			return url;
			    	},
			    	"data" : function(node){
			    		if(node.original && node.original.virtualPath){
			    			return {"token": getCsrfToken(), "path": node.original.virtualPath};
			    		}
			    		return {"token": getCsrfToken()};
			    	},
			    	"success": function (data) {
		            	$.each(data, function(index, object){
		            		if((object.fileType == 'FOLDER' || object.fileType == 'ROOT') && object.children && object.children.length > 0){
		            			$.each(object.children, function(index, children){
				            		if(children.fileType == 'FOLDER'){
				            			children.children = true;
				            		}
				            	});
		            		}else if(object.fileType == 'FOLDER' && !object.children){
		            			object.children = true;
		            		}
		            	});
		            	return data;
			    	},
			    	"error": function(error){
			    		showError(getResource('shareAction.folderNotAccessible'));
			    	}
			    }
			},
			"types" : {
		  		"folder" : {
		  			"icon" : "fa fa-2x fa-folder-o"
		  		},
		  		"pdf" : {
		  			"icon" : "fa fa-2x fa-file-pdf-o"
		  		},
		  		"xls" : {
		  			"icon" : "fa fa-2x fa-file-excel-o"
		  		},
		  		"mp3" : {
		  			"icon" : "fa fa-2x fa-file-sound-o"
		  		},
		  		"doc" : {
		  			"icon" : "fa fa-2x fa-file-word-o"
		  		},
		  		"zip" : {
		  			"icon" : "fa fa-2x fa-file-archive-o"
		  		},
		  		"png" : {
		  			"icon" : "fa fa-2x fa-file-image-o"
		  		},
		  		"txt" : {
		  			"icon" : "fa fa-2x fa-file-text-o"
		  		},
		  		"js" : {
		  			"icon" : "fa fa-2x fa-file-code-o"
		  		},
		  		"ppt" : {
		  			"icon" : "fa fa-2x fa-file-powerpoint-o"
		  		},
		  		"mp4" : {
		  			"icon" : "fa fa-2x fa-file-video-o"
		  		},
		  		"error" : {
		  			"icon" : "fa fa-2x fa-times"
		  		},
		  		"default" : {
		  			"icon" : "fa fa-2x fa-file-file-o"
		  		}
			},
			
			"plugins" : [
				"crrm", "types", "json_data"
			]
		});
		
		$('#' + id + 'TreeView').on("open_node.jstree", function (e, data) {
			data.instance.set_icon(data.node, 'fa fa-2x fa-folder-open-o');
		});
		$('#' + id + 'TreeView').on("close_node.jstree", function (e, data) {
			data.instance.set_icon(data.node, 'fa fa-2x fa-folder-o');
		});
	}

	if(options.disabled) {
		callback.disable();
	}
	
	$(this).data('created', true);
	$(this).data('widget', callback);
	$(this).addClass('widget');
	return callback;

}