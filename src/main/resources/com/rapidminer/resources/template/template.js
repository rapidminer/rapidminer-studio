$(function() {
	$("#tabs").tabs(); 
	$("#tabs").css("float", "left");
	
	$(".help-text").dialog( { autoOpen:false, modal:true, width:900, height: 700 });
	
	$("#input-help").button().click(function() {
		$("#template-description-general").dialog("open");
	});
	$("#results-help").button().click(function() {
		$("#template-description-results").dialog("open");
	});
})