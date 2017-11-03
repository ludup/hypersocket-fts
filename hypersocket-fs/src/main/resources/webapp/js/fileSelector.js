$.fn.fileSelector = function(data) {
	
	var fileTypes = {
	  		// Folder
			"folder" : {
	  			"icon" : "fa fa-2x fa-folder-o"
	  		},
	  		// Documents
	  		"doc" : {
	  			"icon" : "fa fa-2x fa-file-word-o"
	  		},
	  		"docx" : {
	  			"icon" : "fa fa-2x fa-file-word-o"
	  		},
	  		"odt" : {
	  			"icon" : "fa fa-2x fa-file-word-o"
	  		},
	  		"pdf" : {
	  			"icon" : "fa fa-2x fa-file-pdf-o"
	  		},
	  		"ppt" : {
	  			"icon" : "fa fa-2x fa-file-powerpoint-o"
	  		},
	  		// Spreadsheet
	  		"xls" : {
	  			"icon" : "fa fa-2x fa-file-excel-o"
	  		},
	  		"ods" : {
	  			"icon" : "fa fa-2x fa-file-excel-o"
	  		},
	  		"xlr" : {
	  			"icon" : "fa fa-2x fa-file-excel-o"
	  		},
	  		"xlsx" : {
	  			"icon" : "fa fa-2x fa-file-excel-o"
	  		},
	  		// Sound
	  		"mp3" : {
	  			"icon" : "fa fa-2x fa-file-sound-o"
	  		},
	  		"mid" : {
	  			"icon" : "fa fa-2x fa-file-sound-o"
	  		},
	  		"midi" : {
	  			"icon" : "fa fa-2x fa-file-sound-o"
	  		},
	  		"ogg" : {
	  			"icon" : "fa fa-2x fa-file-sound-o"
	  		},
	  		"wav" : {
	  			"icon" : "fa fa-2x fa-file-sound-o"
	  		},
	  		"wma" : {
	  			"icon" : "fa fa-2x fa-file-sound-o"
	  		},
	  		"mpa" : {
	  			"icon" : "fa fa-2x fa-file-sound-o"
	  		},
	  		// Compressed
	  		"zip" : {
	  			"icon" : "fa fa-2x fa-file-archive-o"
	  		},
	  		"rar" : {
	  			"icon" : "fa fa-2x fa-file-archive-o"
	  		},
	  		"7z" : {
	  			"icon" : "fa fa-2x fa-file-archive-o"
	  		},
	  		"tar" : {
	  			"icon" : "fa fa-2x fa-file-archive-o"
	  		},
	  		"gz" : {
	  			"icon" : "fa fa-2x fa-file-archive-o"
	  		},
	  		// Images
	  		"png" : {
	  			"icon" : "fa fa-2x fa-file-image-o"
	  		},
	  		"tif" : {
	  			"icon" : "fa fa-2x fa-file-image-o"
	  		},
	  		"tiff" : {
	  			"icon" : "fa fa-2x fa-file-image-o"
	  		},
	  		"bmp" : {
	  			"icon" : "fa fa-2x fa-file-image-o"
	  		},
	  		"gif" : {
	  			"icon" : "fa fa-2x fa-file-image-o"
	  		},
	  		"ico" : {
	  			"icon" : "fa fa-2x fa-file-image-o"
	  		},
	  		"jpg" : {
	  			"icon" : "fa fa-2x fa-file-image-o"
	  		},
	  		"jpeg" : {
	  			"icon" : "fa fa-2x fa-file-image-o"
	  		},
	  		"svg" : {
	  			"icon" : "fa fa-2x fa-file-image-o"
	  		},
	  		// Plain text
	  		"txt" : {
	  			"icon" : "fa fa-2x fa-file-text-o"
	  		},
	  		"log" : {
	  			"icon" : "fa fa-2x fa-file-text-o"
	  		},
	  		// Code
	  		"js" : {
	  			"icon" : "fa fa-2x fa-file-code-o"
	  		},
	  		"css" : {
	  			"icon" : "fa fa-2x fa-file-code-o"
	  		},
	  		"html" : {
	  			"icon" : "fa fa-2x fa-file-code-o"
	  		},
	  		"xml" : {
	  			"icon" : "fa fa-2x fa-file-code-o"
	  		},
	  		"xhtml" : {
	  			"icon" : "fa fa-2x fa-file-code-o"
	  		},
	  		"java" : {
	  			"icon" : "fa fa-2x fa-file-code-o"
	  		},
	  		"jsp" : {
	  			"icon" : "fa fa-2x fa-file-code-o"
	  		},
	  		"php" : {
	  			"icon" : "fa fa-2x fa-file-code-o"
	  		},
	  		"py" : {
	  			"icon" : "fa fa-2x fa-file-code-o"
	  		},
	  		"asp" : {
	  			"icon" : "fa fa-2x fa-file-code-o"
	  		},
	  		"aspx" : {
	  			"icon" : "fa fa-2x fa-file-code-o"
	  		},
	  		// Video
	  		"mp4" : {
	  			"icon" : "fa fa-2x fa-file-video-o"
	  		},
	  		"avi" : {
	  			"icon" : "fa fa-2x fa-file-video-o"
	  		},
	  		"flv" : {
	  			"icon" : "fa fa-2x fa-file-video-o"
	  		},
	  		"m4p" : {
	  			"icon" : "fa fa-2x fa-file-video-o"
	  		},
	  		"mkv" : {
	  			"icon" : "fa fa-2x fa-file-video-o"
	  		},
	  		"mop" : {
	  			"icon" : "fa fa-2x fa-file-video-o"
	  		},
	  		"mpg" : {
	  			"icon" : "fa fa-2x fa-file-video-o"
	  		},
	  		"mpeg" : {
	  			"icon" : "fa fa-2x fa-file-video-o"
	  		},
	  		// Default
	  		"error" : {
	  			"icon" : "fa fa-2x fa-times"
	  		},
	  		"default" : {
	  			"icon" : "fa fa-2x fa-file-o"
	  		}
		};
	var selectedVirtualPath;
	var id = $(this).attr('id');
	
	if ($(this).data('created')) {
		options = $(this).widget().options();
		return;
	} else {

		var options = $.extend(
		{	 
			disabled : false,
			fileTreeResourceKey: 'text.fileSelector',
			type: 'any', // file, folder or any
		}, data);
		
		var openPath = function(node, pathArray){
			var nodeName = pathArray[0];
			$('#' + id + 'TreeView').jstree("open_node", node);
			$.each(node.children, function(nodeIndex, childNode){
				if(childNode.text == nodeName && childNode.type == 'folder' && pathArray.length > 1){
					$('#' + id + 'TreeView').jstree("open_node", childNode, function(child){
						var nodeData = $('#' + id + 'TreeView').jstree(true).get_json(child.id, {flat:false});
						pathArray.shift();
						openPath(nodeData, pathArray);
					});
					return false;
				}else if(childNode.text == nodeName && ((childNode.type != 'folder') || (childNode.type == 'folder' && pathArray.length == 1))){
					$('#' + id + 'TreeView').jstree('select_node', childNode.id);
					return false;
				}	
			});
		}

		var selectNode = function(path){
			selectedVirtualPath = '';
			$('#' + id + 'TreeView').jstree("deselect_all");
			if(path.startsWith('/')){
				path = path.substring(1, path.length);
			}
			if(path.endsWith('/')){
				path = path.substring(0, path.length - 1);
			}
			var pathArray = path.split('/');
			var treeData = $('#' + id + 'TreeView').jstree(true).get_json('#', {flat:false});
			openPath(treeData[0], pathArray);
		};

		var callback = {
				setValue: function(path) {
					selectNode(path);
				},
				getValue: function() {
					return selectedVirtualPath;
				},
				reset: function() {
					
				},
				disable: function() {
					options.disabled = true;
				},
				enable: function() {
					options.disabled = false;
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
				+	'</div>');
		
		$('#' + id + 'TreeView').jstree({
			'conditionalselect' : function (node) {
				if(options.disabled){
					return false;
				}
				if(options.type == 'any'
					|| (options.type == 'folder' && node.type == 'folder')
					|| (options.type == 'file' && node.type != 'folder')){
					return true;
				}else{
					return false;
				}
			},
			"core" : {
				"animation" : 0,
			    "multiple" : false,
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
				            			children.type = 'folder';
				            		}
				            	});
		            		}else if(object.fileType == 'FOLDER' && !object.children){
		            			object.children = true;
		            		}
		            		
		            		if(object.fileType == 'FOLDER' || object.fileType == 'ROOT'){
		            			object.type = 'folder';
		            		}else if(object.text && object.text.length > 0){
		            			var extension = object.text.split('.')[object.text.split('.').length - 1];
		            			if(fileTypes.hasOwnProperty(extension)){
			            			object.type = extension;
			            		}else{
			            			object.type = 'default';
			            		}
		            		}
		            	});
		            	
		            	return data;
			    	},
			    	"error": function(error){
			    		showError(getResource('shareAction.folderNotAccessible'));
			    	}
			    }
			},
			"types" : fileTypes,
			
			"plugins" : [
				"crrm", "types", "json_data", "conditionalselect"
			]
		}).on('changed.jstree', function (e, data) {
			if(data.node && selectedVirtualPath != data.node.original.virtualPath){
				selectedVirtualPath = data.node.original.virtualPath;
				if(options.changed) {
		 			options.changed(callback);
		 		}
			}
		}).bind('ready.jstree', function(e, data) {
			if(options.value){
    			selectNode(options.value);
    		}
		});
		
		(function ($, undefined) {
			"use strict";
			$.jstree.defaults.conditionalselect = function () { return true; };
			$.jstree.plugins.conditionalselect = function (options, parent) {
//				own function
				this.select_node = function (obj, supress_event, prevent_open) {
					if(this.settings.conditionalselect.call(this, this.get_node(obj))) {
						parent.select_node.call(this, obj, supress_event, prevent_open);
					}
				};
			};
		})(jQuery);
		
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