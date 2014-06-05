<%@ page language="java" contentType="text/html; charset=US-ASCII" pageEncoding="US-ASCII"%>
<%@ page import="java.util.*" %>
<%@ page import="java.text.*" %>
	
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=US-ASCII">
<title>FSB - FAX Status Board</title>

<link rel="stylesheet" href="css/ilija.css" type="text/css">
<link rel="stylesheet"
	href="http://code.jquery.com/ui/1.10.2/themes/redmond/jquery-ui.css">
<link rel="stylesheet" href="DataTables-1.9.4/media/css/demo_table.css">

<script src="//code.jquery.com/jquery-1.11.0.min.js"></script>
<script src="http://code.jquery.com/ui/1.10.2/jquery-ui.js"></script>

<script type="text/javascript" language="javascript"
	src="DataTables-1.9.4/media/js/jquery.dataTables.js"></script>
	
<script src="http://code.highcharts.com/highcharts.js"></script>
<script src="http://code.highcharts.com/modules/heatmap.js"></script>

<script
	src="https://maps.googleapis.com/maps/api/js?v=3.exp&sensor=false"></script>

</head>
<body>

    
    <% 
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
	cal.add(Calendar.HOUR_OF_DAY, +1 );
	Date date = cal.getTime();
	String dateFormat = "yyyy-MM-dd";
	String hourFormat = "HH00";
	SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
	SimpleDateFormat shf = new SimpleDateFormat(hourFormat);
	sdf.setTimeZone(cal.getTimeZone());
	shf.setTimeZone(cal.getTimeZone());
    String fDate=sdf.format(date) + "T" + shf.format(date);
    %>
    
    <script>
    fDate="<%= fDate %>";
    </script>
    


	<script>
		var epTable, reTable;


		function getepStatusTable() {
			$("#loading").show();
			$.get("wanio", {}, function(msg) {
				if (msg.length) {
					msg = msg.trim().split(/\n/g);
					jQuery.each(msg, function(i, line) {
						line = line.split(/\t/);
						// name, direct, upstream. downstream, x509
						epTable.fnAddData([ '<img src="images/details_open.png">', line[0], line[1], line[2], line[3], line[4] ]);
					});
				}
			});
			$("#loading").hide();
		}

		

		var map; 
		var markers=[];
		var geodesic;
		var geodesics=[];
		var geodesicOptions = { strokeColor: '#CC0099', strokeOpacity: 1.0, strokeWeight: 3, geodesic: true }
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
 			$.get("wancost", {
				preload : "all"
			}, function(msg) {
				if (msg.length) {
					msg = msg.trim().split(/\n/g);
					jQuery.each(msg, function(i, line) {
						line = line.split(/\t/);
						if (line[0] == 'source')
							$("#idSource").append( '<option value="'+line[1]+'">' + line[1] + '</option>');
						if (line[0] == 'destination')
							$("#idDestination").append( '<option value="'+line[1]+'">' + line[1] + '</option>');
					});
				}
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
			$("#loading").hide();
		}

		function showCostMatrix(){
			$("#loading").show();
			hcoptions={
			        chart: { type: 'heatmap', marginTop: 40, marginBottom: 40 },
			        title: { text: 'xrdcp rates from FAX endpoints to WNs'},
			        xAxis: { categories:[], title: 'destinations' },
			        yAxis: { categories:[], title: 'sources' },
			        colorAxis: { stops: [ [0, '#ff0000'], [0.05, '#00ff00'], [1, '#0000FF'] ], min: 0 },
			        legend: { align: 'right', layout: 'vertical', margin: 0, verticalAlign: 'top', y: 25, symbolHeight: 320 },
			        tooltip: {
			            formatter: function () {
			                return '<b>' + this.series.xAxis.categories[this.point.x] + '</b> copied <br><b>' +
			                    this.point.value + '</b> in average from <br><b>' + this.series.yAxis.categories[this.point.y] + '</b>';
			            }
			        },
			        series: [{
			            name: 'xrdcp rates', borderWidth: 1, data: [],
			            dataLabels: { enabled: false, color: 'black', style: { textShadow: 'none' } }
			        }]
			}
			  			
			$.get("wancost",{costmatrix:$("#idTimeScale option:selected").val()}, function(msg){
				if (msg.length) {
					hcoptions.xAxis.categories=[]
					hcoptions.yAxis.categories=[]
					msg = msg.trim().split(/\n/g);
					line=msg[0].split(/\t/);
					l=1;
					for (i=0;i<parseInt(line[1]);i++){ //sources
						for (j=0;j<parseInt(line[2]);j++){ // destinations
							dline=msg[l].split(/\t/);
							if(i==0) hcoptions.xAxis.categories.push(dline[1]); //destination on x axis
							if(j==0) hcoptions.yAxis.categories.push(dline[0]); // source on y axis
							var z=parseFloat(dline[2]);
							if (z>=0) hcoptions.series[0].data.push( [ j, i, z ] );
							l++;
						}
					}
					$('#costmatrixspace').highcharts(hcoptions);
				}
			} );
			$("#loading").hide();
		}
		
		window.onload = function() {
			//	alert("welcome");
		}

		$(document).ready(
				function() {

					getepStatusTable();
					preload();
					$('#idSource, #idDestination, #idTimeZoom').change(showCost);
					$('#idMapTimeScale').change(showCost);
					$('#idTimeScale').change(showMap);
					
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
								// getStatusDoneTable();
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

						}
					});

					epTable = $('#epStatus').dataTable( { 
						"iDisplayLength" : 25,
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
						"iDisplayLength" : 25,
						"aoColumnDefs" : [ {
							"bSortable" : false,
							"aTargets" : [ 0 ]
						} ],
						"aaSorting" : [ [ 1, 'asc' ] ],
						"aoColumns" : [ {
							sWidth : '3%'
						}, {
							sWidth : '6%',
							"sClass" : "right"
						}, {}, {}, {
							sWidth : '3%'
						} ]

					});

				});
	</script>

	<div class="maincolumn">

		<div class="mainheading">
			<a href="http://atlas.web.cern.ch/Atlas/Collaboration/"> <img
				border="0" src="images/atlas_logo.jpg" alt="ATLAS main page">
			</a>
			<div id="maintitle">FAX Status Board</div>
		</div>


		<div id="tabs">
			<ul>
				<li><a href="#tabs-1">Endpoints Status</a></li>
				<li><a href="#tabs-2">Redirectors Status</a></li>
				<li><a href="#tabs-3">Map</a></li>
				<li><a href="#tabs-4">Link rates</a></li>
				<li><a href="#tabs-5">Cost Matrix</a></li>
			</ul>
			<div id="tabs-1">
				<br>
				<table cellpadding="0" cellspacing="0" border="0" class="display"
					id="epStatus" width="100%">
					<thead>
						<tr>
							<th></th>
							<th>Endpoint</th>
							<th>Direct</th>
							<th>Upstream redirection</th>
							<th>Downstream redirection</th>
							<th>ATLAS readonly</th>
						</tr>
					</thead>
					<tbody></tbody>
				</table>
			</div>

			<div id="tabs-2">
				<br>
				<table cellpadding="0" cellspacing="0" border="0" class="display"
					id="reStatus" width="100%">
					<thead>
						<tr>
							<th></th>
							<th>Redirector</th>
							<th>Upstream redirection</th>
							<th>Downstream redirection</th>
							<th></th>
						</tr>
					</thead>
					<tbody></tbody>
				</table>
			</div>

			<div id="tabs-3"><select id="idMapTimeScale">
				<option value="destination">show rates to site</option>
				<option value="source">show rates from site</option>
				</select> 
				<div id="map-canvas" style="height: 640px"></div>
			</div>
			<div id="tabs-4">
				Source <select id="idSource"></select> 
				Destination <select	id="idDestination"></select>
				Time bin:<select id="idTimeZoom">
				<option value="FAXcost">no Binning</option>
				<option value="FAXcost30min"  selected>30 min</option>
				<option value="FAXcost1h">1h</option>
				<option value="FAXcost3h">3h</option>
				<option value="FAXcost6h">6h</option>
				<option value="FAXcost12h">12h</option>
				<option value="FAXcost24h">24h</option>
				</select> 
				<br>
				<div id="graphspace"></div>
			</div>			
			<div id="tabs-5">
				Time scale:<select id="idTimeScale">
				<option value=6>6h</option>
				<option value=12>12h</option>
				<option value=24 selected>24h </option>
				<option value=48>48h</option>
				<option value=168>last week</option>
				<option value=336>last 2 weeks</option>
				<option value=672>last month</option>
				<option value=8760>last year</option>
				<option value=99999>all the time</option>
				</select> 
				<div id="costmatrixspace" style="height:800px"></div>
			</div>
		</div>

	</div>

	<div id="loading" style="display: none">
		<br> <br>Loading data. Please wait...<br> <br> <img
			src="images/wait_animated.gif" alt="loading" />
	</div>
</body>
</html>