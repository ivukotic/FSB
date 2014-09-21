
var epTable, reTable, failoverTable;


function getepStatusTable() { 
	$("#loading").show();
	$.get("wanio", {endpoint:""}, function(msg) {
		if (msg.length) {
			msg = msg.trim().split(/\n/g);
			jQuery.each(msg, function(i, line) {
				line = line.split(/\t/);
				// name, direct, upstream. downstream, x509
				epTable.fnAddData([ '<img src="static/details_open.png">', line[0], line[1], line[2], line[3], line[4] ]);
			});
		}
	});
	$("#loading").hide();
}

function getreStatusTable() {
	$("#loading").show();
	$.get("wanio", {redirector:""}, function(msg) {
		if (msg.length) {
			msg = msg.trim().split(/\n/g);
			jQuery.each(msg, function(i, line) {
				line = line.split(/\t/);
				// name, upstream. downstream
				reTable.fnAddData([ '<img src="static/details_open.png">', line[0], line[1], line[2], line[3]]);
			});
		}
	});
	$("#loading").hide();
}


var map; 
var markers=[];
var geodesic;
var geodesics=[];
var geodesicOptions = { strokeColor: '#CC0099', strokeOpacity: 1.0, strokeWeight: 3, geodesic: true };
var infowindow;
function drawConnections(marker){
	//clear paths
	for (var i=0;i<geodesics.length;i++) geodesics[i].setMap(null);

	map.panTo(marker.getPosition());
	var stars=[];
	var rates=[];
	$.get("wancost",{central:marker.title, as:$("#idMapTimeScale option:selected").val() }, function(msg){
		if (msg.length) {
			msg = msg.trim().split(/\n/g);
			var maxRate=0;
			jQuery.each(msg, function(i, line) {
				line = line.split(/\t/);
				stars.push(line[0]);
				var rate=parseFloat(line[1])
				rates.push(rate);
				if (rate>maxRate) maxRate=rate;
			});

			console.log("stars: "+stars.length);
			console.log("marks: "+markers.length);
			if (stars.length==1) {
				alert("Site "+marker.title+" was not used as "+$("#idMapTimeScale option:selected").val());
				return;
			}

			for (var s in stars){ // these are only site names
				for (var i=0;i<markers.length;i++){ // these are actual markers
					sitename=markers[i].title;
					if(sitename!=stars[s]) continue;
					geodesic = new google.maps.Polyline(geodesicOptions);
					geodesic.setOptions({strokeWeight: rates[s]/maxRate*5.0}); 
					geodesic.setMap(map);

					google.maps.event.addListener(geodesic, 'click', function(event) {
						cont="";
						if ($("#idMapTimeScale option:selected").val()=="destination")
							cont="source:<b>"+sitename+"</b><br>destination: <b>"+marker.title+"</b><br>rate: <b>"+rates[s]+"</b>";
						else
							cont="destination:<b>"+sitename+"</b><br>source: <b>"+marker.title+"</b><br>rate: <b>"+rates[s]+"</b>";
						infowindow = new google.maps.InfoWindow({content: cont});
						infowindow.open(map,event.latLng);
					});
					var gPath = geodesic.getPath();
					gPath.push(marker.getPosition());
					gPath.push(markers[i].getPosition());
					geodesics.push(geodesic);

				}
			}

		}
	} );


}

function showMap() {

	$("#loading").show();
	var mapOptions = { zoom : 2, center : new google.maps.LatLng(20.0, 0.0) };
	map = new google.maps.Map(document.getElementById('map-canvas'), mapOptions);

	$.get("wanio", {
		map : "all"
	}, function(msg) {
		if (msg.length) {
			markers=[];
			msg = msg.trim().split(/\n/g);
			jQuery.each(msg, function(i, line) {
				line = line.split(/\t/);

				var marker = new google.maps.Marker({
					position : new google.maps.LatLng( parseFloat(line[1]), parseFloat(line[2])), 
					map : map, title : line[0]
				});

				google.maps.event.addListener(marker, 'click', function(){drawConnections(marker);});

				markers.push(marker);
			});
		}
	});
	$("#loading").hide();
}



function preload() {
	$.getJSON("wancost", { preload : "all" }, function(data) {
		s=data.sources;
		d=data.destinations;
		for (i in s)	$("#idSource").append( '<option value="'+s[i]+'">' + s[i] + '</option>');
		for (i in d)	$("#idDestination").append( '<option value="'+d[i]+'">' + d[i] + '</option>');
	});
	$.getJSON("CopyOverflow", {preload : "all"}, function(data) {
		for (i in data)	$("#idOverflowDestinationSelect").append( '<option value="'+data[i]+'">' + data[i] + '</option>');
	});
	$.getJSON("UpdateFailover", {preload : "all"}, function(data) {
		for (i in data)	$("#idFailoverSiteSelect").append( '<option value="'+data[i]+'">' + data[i] + '</option>');
	});
	
}

function showCost() {
	$("#loading").show();
	var options = {
			chart : {
				renderTo : 'graphspace', zoomType : 'xy',
				type : 'line', height : 600, margin : [ 60, 30, 45, 70 ]
			},
			title : { text : '' },
			xAxis : { type : 'datetime', tickWidth : 0, gridLineWidth : 1, title : { text : 'Date' } },
			yAxis : { title : { text : 'Rate [MB/s]' } },
			legend : { align : 'left', verticalAlign : 'top', y : 10, floating : true, borderWidth : 0 },
			exporting: {     buttons: { contextButton: {  text: 'Export' } }, sourceHeight:1050, sourceWidth: 1485    },
			credits: { enabled: false }
			// tooltip: { formatter: function() { return '<b>'+ this.series.name +'</b><br/>'+ Highcharts.dateFormat('%e. %b', this.x) +': '+ this.y +' m';} },
	}; 
	$.get("wancost",{source:$("#idSource option:selected").text(),destination:$("#idDestination option:selected").text(),binning:$("#idTimeZoom option:selected").val()}, function(msg){
		if (msg.length) {
			options.series = new Array();
			options.series[0]=new Object();
			options.series[0].name = $("#idSource option:selected").text()+" to "+ $("#idDestination option:selected").text();
			options.series[0].data = new Array();
			msg = msg.trim().split(/\n/g);
			jQuery.each(msg, function(i, line) {
				line = line.split(/\t/);
				var x=parseFloat(line[0]);
				var y=parseFloat(line[1]);
				options.series[0].data.push( [ x, y ] );
			});
			chart = new Highcharts.Chart(options); 
		}
	} ); 
	showGraph();
	$("#loading").hide();
}

function showStability(){

	$.ajaxSetup({async: false});
	
	$("#loading").show();
	hcoptions={
			chart: { type: 'heatmap', marginTop: 40, marginBottom: 40 },
			title: { text: 'Stability of ' + $("#idStability option:selected").val() + ' services in the last '+$("#idStabilityTimeScale option:selected").val()+' hours'},
			xAxis: { categories:[], title: 'time' },
			yAxis: { categories:[], title: 'endpoint' },
			legend: { enabled: false },
			exporting: { buttons: { contextButton: {  text: 'Export' }  }, sourceHeight:1050, sourceWidth: 1485 },
			credits: { enabled: false },
			colorAxis: { minColor: '#ff0000', maxColor: '#00ff00',min:0, max:1 },
			tooltip: {
				formatter: function () {
					sname=this.series.yAxis.categories[this.point.y];
					sel=$("#idStability option:selected").val();
					if (sel=='direct') link=sname +'_to_' +sname;
					if (sel=='upstream') link='upstreamFrom_' +sname;
					if (sel=='downstream') link='downstreamTo_' +sname;
					if (sel=='x509') link='checkSecurity_' +sname;
					link='http://www.mwt2.org/ssb/'+link+'_'+ this.series.xAxis.categories[this.point.x]+'.log';
					return '<b>' + sname + '<br>' + this.series.xAxis.categories[this.point.x] + '<br></b>'+
					'<a href="' + link + '">see log file</a>';
				}
			},
			series: [{
				name: 'status', borderWidth: 1, data: [],
				dataLabels: { enabled: false, color: 'black', style: { textShadow: 'none' } }
			}]
	}

	$.get("wanio",{service:$("#idStability option:selected").val(),timescale:$("#idStabilityTimeScale option:selected").val()}, function(msg){
		if (msg.length) {
			msg = msg.trim().split(/\n/g);
			line=msg[0].split(/\t/);
			sites=line[0];
			moms=line[1];

			site=msg[1].split(/\t/); // next line contain names of sites
			for (i=0;i<sites;i++) {
				hcoptions.yAxis.categories.push(site[i]);
				//console.log(line[i]);
			}
			console.log("sites: "+hcoptions.yAxis.categories.length);
			
			mom=msg[2].split(/\t/); // next line contains momemnts
			for (i=0;i<moms;i++){
				hcoptions.xAxis.categories.push(mom[i]);
				//console.log(line[i]);
			}
			console.log("moments: "+hcoptions.xAxis.categories.length);

			console.log("data: "+(msg.length-3)); // the rest of the lines are in format: site index, moment index, status
			for (ms=3;ms<msg.length;ms++){
				line=msg[ms].split(/\t/);
				hcoptions.series[0].data.push( [ parseInt(line[1]), parseInt(line[0]), parseFloat(line[2]) ] );
				//console.log(msg[ms]+ "\tmom:"+line[1]+"\tsite:"+line[0]+"\t"+site[line[0]]+'\t'+mom[line[1]] +'\t'+ line[2]);
			} 
			$('#stabilityspace').highcharts(hcoptions);
		}
	} );
	$("#loading").hide();
}

window.onload = function() {
	//	alert("welcome");
}


$(document).ready( function(){
	preload();
	$('#idSource, #idDestination, #idTimeZoom').change(showCost);
	$('#idMapTimeScale').change(showMap);
	$('#idCostMatrixTimeScale').change(showCostMatrix);
	$('#idStability, #idStabilityTimeScale').change(showStability);
	$('#idFailoverTimeScale, #idFailoverPlotSelect, #idFailoverSiteSelect').change(showFailover);
	$('#idOverflowTimeScale, #idOverflowPlotSelect, #idOverflowDestinationSelect').change(showOverflow);

	getepStatusTable();

	
	$("#tabs").tabs({
		activate : function(event, ui) {
			if (ui.newPanel.index() == 1) { // loading status tab
				console.log("loading tab 1.");
				$("#loading").show();
				getepStatusTable();
				$("#loading").hide();
			} else {
				epTable.fnClearTable();
			}

			if (ui.newPanel.index() == 2) { // loading redirector tab
				console.log("loading tab 2.");
				$("#loading").show();
				getreStatusTable();
				$("#loading").hide();
			} else {
				reTable.fnClearTable();
			}

			if (ui.newPanel.index() == 3) { // map
				console.log("loading tab 3.");
				$("#loading").show();
				showMap();
				$("#loading").hide();
			} else {

			}
			if (ui.newPanel.index() == 4) { // loading single link plot
				console.log("loading tab 4.");
				$("#loading").show();
				showCost();
				$("#loading").hide();
			} else {

			}							
			if (ui.newPanel.index() == 5) { // loading cost matrix
				console.log("loading tab 5.");
				$("#loading").show();
				showCostMatrix();
				$("#loading").hide();
			} else {

			}							
			if (ui.newPanel.index() == 6) { // loading stability
				console.log("loading tab 6.");
				$("#loading").show();
				showStability();
				$("#loading").hide();
			} else {

			}							
			if (ui.newPanel.index() == 7) { // loading failover
				console.log("loading tab 7.");
				$("#loading").show();
				showFailover();
				$("#loading").hide();
			} else {

			}							
			if (ui.newPanel.index() == 8) { // loading overflow
				console.log("loading tab 8.");
				$("#loading").show();
				showOverflow();
				$("#loading").hide();
			} else {

			}
			
			if (ui.newPanel.index() == 9) { // map traceroutes
				console.log("loading tab 9.");
				$("#loading").show();
				showTraceroutes();
				$("#loading").hide();
			} else {

			}
			if (ui.newPanel.index() == 11) { // topology
				console.log("loading tab 11.");
				$("#loading").show();
				showTopology();
				$("#loading").hide();
			} else {

			}			
			
		}
	});

	epTable = $('#epStatus').dataTable( {
    	"bJQueryUI": true,
		"iDisplayLength" : 100,
		"aoColumnDefs" : [ { "bSortable" : false, "aTargets" : [ 0 ] } ],
		"aaSorting" : [ [ 1, 'asc' ] ],
		"aoColumns" : [ { sWidth : '3%' }, { sWidth : '20%' }, {}, {}, {}, {} ],
		"fnRowCallback" : function(nRow, aData, iDisplayIndex, iDisplayIndexFull) {

			sname=aData[1]
			for (col = 2; col < 6; col++){
				link=''
					if (col==2) link=sname +'_to_' +sname
					if (col==3) link='upstreamFrom_' +sname
					if (col==4) link='downstreamTo_' +sname
					if (col==5) link='checkSecurity_'+sname
					link+='_'+fDate+'.log'

					if (aData[col] == "true") {
						$('td:eq(' + col + ')', nRow).html('<a href="http://www.mwt2.org/ssb/' + link + '">OK</a>').css( 'backgroundColor', '#00FF00');
					} else
						$('td:eq(' + col + ')', nRow).html('<a href="http://www.mwt2.org/ssb/'+link+'">Problem</a>').css( 'backgroundColor', '#FF0000');

				if (aData[5] == "false")
					$('td:eq(5)', nRow).css( 'backgroundColor', '#FFFF00');
			} 
		}
	});

	reTable = $('#reStatus').dataTable({
    	"bJQueryUI": true,
		"iDisplayLength" : 25,
		"aoColumnDefs" : [ { "bSortable" : false, "aTargets" : [ 0 ] } ],
		"aaSorting" : [ [ 1, 'asc' ] ],
		"aoColumns" : [ { sWidth : '3%' }, {}, {sWidth : '30%'},  {sWidth : '30%'} ],
		"fnRowCallback" : function(nRow, aData, iDisplayIndex, iDisplayIndexFull) {

			sname=aData[1];
			for (col = 2; col < 4; col++){
				link=''
					if (col==2) link='checkRedirectorUpstream_' +sname;
				if (col==3) link='checkRedirectorDownstream_' +sname;
				link+='_'+fDate+'.log'

				if (aData[5] == "true"){
					$('td:eq(3)', nRow).html('can not check').css( 'backgroundColor', '#FFFF00');
					$('td:eq(4)', nRow).html('can not check').css( 'backgroundColor', '#FFFF00');
					continue;
				}

				if (aData[col] == "false") {
					$('td:eq(' + col + ')', nRow).html('<a href="http://www.mwt2.org/ssb/' + link + '">OK</a>').css( 'backgroundColor', '#00FF00');
				} else
					$('td:eq(' + col + ')', nRow).html('<a href="http://www.mwt2.org/ssb/'+link+'">Problem</a>').css( 'backgroundColor', '#FF0000');


			} 
		}

	});
	
	failoverTable = $('#failoverspace').dataTable( {
    	"bJQueryUI": true,
		"iDisplayLength" : 100,
		"aoColumnDefs" : [ { "bSortable" : false, "aTargets" : [ 0 ] } ],
		"aaSorting" : [ [ 1, 'asc' ] ],
		"aoColumns" : [ { sWidth : '3%' }, { sWidth : '15%' }, {}, {}, {}, {}, {}, {},{},{} ],
		"fnRowCallback" : function(nRow, aData, iDisplayIndex, iDisplayIndexFull) {
			$('td:eq(' + 3 + ')', nRow).html('<a href="http://pandamon.cern.ch/jobinfo?job=' + aData[3] + '">'+aData[3]+'</a>');
		}
	});

	
});
