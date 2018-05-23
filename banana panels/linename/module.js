/*
  ## linename Panel
*/
define([
  'angular',
  'app',
  'underscore',
  'jquery',
  'd3'
], function(angular, app, _, $, d3) {
  'use strict';

  var module = angular.module('kibana.panels.linename', []);
  app.useModule(module);

  module.controller('linename', function($scope, $timeout, timer, dashboard, querySrv, filterSrv) {
    $scope.panelMeta = {
      modals: [{
        description: "Inspect",
        icon: "icon-info-sign",
        partial: "app/partials/inspector.html",
        show: $scope.panel.spyable
      }],
      editorTabs: [{
        title: 'Queries',
        src: 'app/partials/querySelect.html'
      }],
      status: "Experimental",
      description: "linename chart panel currently support only plotting data of the same field type. You have to define which fields to be plotted on Y-axis fields. Data must have X-axis as timestamp and Y-axis must have values, if not it will be discarded."
    };

    // default values
    var _d = {
      queries: {
          mode: 'all',
          ids: [],
          query: '*:*',
          custom: ''
      },
      size: 1000,
      max_rows: 10000, // maximum number of rows returned from Solr
      field: 'timestamp',
      yAxis: '',
      right_yAxis: '',
      fl: '',
      right_fl: '',
      spyable: true,
      show_queries: true,
      interpolate: 'basis',
      right_interpolate: 'basis',
      rightYEnabled: false,
      showLegend: true,
      showRightLegend: true,
      refresh: {
        enable: false,
        interval: 2
      }
    };

    _.defaults($scope.panel, _d);

    $scope.init = function() {
      // Start refresh timer if enabled
      if ($scope.panel.refresh.enable) {
        $scope.set_timer($scope.panel.refresh.interval);
      }

      $scope.$on('refresh', function() {
        $scope.get_data();
      });
      $scope.get_data();
    };

    $scope.set_timer = function(refresh_interval) {
      $scope.panel.refresh.interval = refresh_interval;
      if (_.isNumber($scope.panel.refresh.interval)) {
        timer.cancel($scope.refresh_timer);
        $scope.realtime();
      } else {
        timer.cancel($scope.refresh_timer);
      }
    };

    $scope.realtime = function() {
      if ($scope.panel.refresh.enable) {
        timer.cancel($scope.refresh_timer);

        $scope.refresh_timer = timer.register($timeout(function() {
          $scope.realtime();
          $scope.get_data();
        }, $scope.panel.refresh.interval*1000));
      } else {
        timer.cancel($scope.refresh_timer);
      }
    };

    $scope.get_data = function() {
      // Show progress by displaying a spinning wheel icon on panel
      delete $scope.panel.error;
      $scope.panelMeta.loading = true;

      // Set Solr server
      $scope.sjs.client.server(dashboard.current.solr.server + dashboard.current.solr.core_name);

      // Construct Solr query
      var request = $scope.sjs.Request();
      var fq = '';
      if (filterSrv.getSolrFq()) {
          fq = '&' + filterSrv.getSolrFq();
      }
      var wt_json = '&wt=json';
      // NOTE: $scope.panel.field is the time field for x-Axis
      // TODO: need to rename to $scope.panel.timefield
      var fl = '&fl=' + $scope.panel.field + ',' + $scope.panel.fl + ',' + $scope.panel.split;
      if ($scope.panel.right_fl) {
          fl += ',' + $scope.panel.right_fl;
      }
      var rows_limit = '&rows=' + $scope.panel.max_rows;
      var sort = '&sort=' + $scope.panel.field + ' asc';

      $scope.panel.queries.query = querySrv.getORquery() + fq + fl + wt_json + rows_limit + sort;

      // Set the additional custom query
      if ($scope.panel.queries.custom != null) {
          request = request.setQuery($scope.panel.queries.query + $scope.panel.queries.custom);
      } else {
          request = request.setQuery($scope.panel.queries.query);
      }

      // Execute the search and get results
      var results = request.doSearch();

      // Populate scope when we have results
      results.then(function (results) {
          if(!(_.isUndefined(results.error))) {
              $scope.panel.error = $scope.parse_error(results.error.msg);
              return;
          }
          // build $scope.data array
          $scope.panelMeta.loading = false;
          $scope.data = [];

          if (!(_.isUndefined(results.error))) {
              $scope.panel.error = $scope.parse_error(results.error.msg);
              $scope.render();
              return;
          }

          $scope.data = results.response.docs;
          $scope.render();
      });

      // Hide the spinning wheel icon
      $scope.panelMeta.loading = false;
    };

    $scope.set_refresh = function(state) {
      $scope.refresh = state;
    };

    $scope.close_edit = function() {
      // Start refresh timer if enabled
      if ($scope.panel.refresh.enable) {
        $scope.set_timer($scope.panel.refresh.interval);
      }
      if ($scope.refresh) {
        $scope.get_data();
      }
      $scope.refresh = false;
      $scope.$emit('render');
    };

    $scope.render = function() {
      $scope.$emit('render');
    };

    $scope.populate_modal = function(request) {
      $scope.inspector = angular.toJson(JSON.parse(request.toString()), true);
    };

    $scope.pad = function(n) {
      return (n < 10 ? '0' : '') + n;
    };
  });

  module.directive('linenameChart', function() {
    return {
      restrict: 'E',
      link: function(scope, element) {

        scope.$on('render', function() {
            render_panel();
        });

        angular.element(window).bind('resize', function() {
            render_panel();
        });

        // Function for rendering panel
        function render_panel() {

          element.html("");

          var el = element[0];
          // deepcopy of the data in the scope
          var data;
          data = jQuery.extend(true, [], scope.data); // jshint ignore: line

          if (d3.keys(data[0]).indexOf(scope.panel.field) === -1) {
              return;
          }
		  var moddata = ""
			for (var Dkey in data) {
				if(moddata.length == 0)
				{
					var moddata = [{ball:data[Dkey][scope.panel.field][0],[data[Dkey][scope.panel.split]]:data[Dkey][scope.panel.fl][0]}]
				}
				else
				{
					var isSet = false;
					for (var key in moddata) {
						if(moddata[key].ball==data[Dkey][scope.panel.field][0]){
							var temp = data[Dkey][scope.panel.split]
							moddata[key][temp] = data[Dkey][scope.panel.fl][0]
							isSet=true;
							break;
						}
					}

					if(!isSet)
					{
						var dt = {ball:data[Dkey][scope.panel.field][0],[data[Dkey][scope.panel.split]]:data[Dkey][scope.panel.fl][0]}
						moddata.push(dt);
					}
				}
			}

          var parent_width = $("#linename").width(),
              parent_height = $('#linename').parent().parent().parent().parent().height();

          var margin = {
                  top: 20,
                  right: 80,
                  bottom: 30,
                  left: 50
              },
              width = parent_width - margin.left - margin.right - 50,
              height = parent_height - margin.top - margin.bottom;

          // The need for two date parsers is that sometimes solr removes the .%L part if it equals 000
          // So double checking to make proper parsing format and cause no error
          var parseDate = d3.time.format.utc("%Y-%m-%dT%H:%M:%S.%LZ");
          var parseDate2 = d3.time.format.utc("%Y-%m-%dT%H:%M:%SZ");

		  data = moddata;
          var isDate = false;
          // Check if x is date or another type
          if (data && data.length > 0) {
              var sample_date = data[0]["ball"];
              isDate = parseDate.parse(String(sample_date)) || parseDate2.parse(String(sample_date));
          }

          // d3 stuffs
          var x;
          if (isDate) {
              x = d3.time.scale().range([0, width]);
          } else {
              x = d3.scale.linear().range([0, width]);
          }

          var y = d3.scale.linear().range([height, 0]);


          var color = d3.scale.category10();

          var xAxis = d3.svg.axis().scale(x).orient("bottom");
          var yAxis = d3.svg.axis().scale(y).orient("left");

          var line = d3.svg.line()
              .interpolate(scope.panel.interpolate) // interpolate option
              .x(function(d) {
                  return x(d.xValue);
              })
              .y(function(d) {
                  return y(d.yValue);
              });


			   var svg = d3.select(el).append("svg")
              .attr("width", width + margin.left + margin.right)
              .attr("height", height + margin.top + margin.bottom)
              .attr("viewBox", "0 0 " + parent_width + " " + parent_height)
              .attr("preserveAspectRatio", "xMidYMid")
              .append("g")
              .attr("transform", "translate(" + margin.left + "," + margin.top + ")");


          // Colors domain must be the same count of fl

          color.domain(d3.keys(data[0]).filter(function(key) {
              return key !== "ball";
          }));

		            var yFields = color.domain().map(function(name) {
              return {
                  name: name,
                  values: data.map(function(d) {
                      return {
                          xValue: d.ball,
                          yValue: +d[name]
                      };
                  })
              };
          });

		            x.domain(d3.extent(data, function(d) {
              return d["ball"];
          }));

          y.domain([
              d3.min(yFields, function(c) {
                  return d3.min(c.values, function(v) {
                      return v.yValue;
                  });
              }),
              d3.max(yFields, function(c) {
                  return d3.max(c.values, function(v) {
                      return v.yValue;
                  });
              })
          ]);

          var y_right,y_right_color,yAxis_right,line_right;



          svg.append("g")
              .attr("class", "x axis")
              .attr("transform", "translate(0," + height + ")")
              .call(xAxis);

          svg.append("g")
              .attr("class", "y axis")
              .call(yAxis)
              .append("text")
              .attr("transform", "rotate(-90)")
              .attr("y", 6)
              .attr("dy", ".71em")
              .style("text-anchor", "end")
              .text(scope.panel.yAxis);

          var yfield = svg.selectAll(".yfield")
              .data(yFields)
              .enter().append("g")
              .attr("class", "yfield");

          yfield.append("path")
              .attr("class", "line")
              .attr("d", function(d) {
                  return line(d.values);
              })
              .style("stroke", function(d) {
                  return color(d.name);
              })
              .style("fill", "transparent");

			  yfield.append("text")
			  .datum(function(d) { return {name: d.name, value: d.values[d.values.length - 1]}; })
			  .attr("transform", function(d) { return "translate(" + x(d.value.xValue) + "," + y(d.value.yValue) + ")"; })
			  .attr("x", 3)
			  .attr("dy", ".35em")
			  .text(function(d) { return d.name; });

			  yfield.selectAll("circle")
				.data(function(d){return d.values})
				.enter()
				.append("circle")
				.attr("r", 3)
				.attr("cx", function(d) { return x(d.xValue); })
				.attr("cy", function(d) { return y(d.yValue); })
				.style("fill", function(d,i,j) { return color(yFields[j].name); });

        }

        render_panel();
      }
    };
  });
});
