'use strict';

angular.module('ruleApp.controllers', [])
	.controller('HomeCtrl', [ '$http', '$scope', function($http, $scope) {
		$scope.$parent.title = '&nbsp;';
		$scope.$parent.menuItems = [
		                            {"text": "Standard Mode", "href": "#/standard/background"},
		                            {"text": "Advanced Mode", "href": "#/advanced"},
		                            {"text": "Revise My Rules", "href": "#/revise"},
		                            {"text": "Use Others' Rules", "href": "#/others"},
		                            {"text": "Graphical Rules", "href": "#/graph"},
		                            {"text": "Help", "href": "#/help"}
		                           ];
	}])
	.controller('BackgroundCtrl', [ '$http', '$scope', function($http, $scope) {
		$scope.$parent.title = 'Background Information';
		$scope.$parent.menuItems = standardMenuItems(0);
		$scope.reference = [{Name: "Guide to clinical Preventive Services 2012", Reference: "http://www.ahrq.gov/professionals/clinicians-providers/guidelines-", Type: "Web"},
		                    {Name: "Diagnosis of functional kidney failure of cirrhosis with Dopple sonography:", Reference: "X&Y Journal; p. 234-246", Type: "Journal"}];
	    $scope.gridOptions = { data: 'reference' };
	}])
	.controller('TriggerCtrl', [ '$http', '$scope', function($http, $scope) {
		$scope.$parent.title = 'Decide how the Rule will be Triggered';
		$scope.$parent.menuItems = standardMenuItems(1);
	}])
	.controller('GraphCtrl', [ '$http', '$scope', function ($http, $scope) {
		$http.get('partials/tree.json').success(function(data) {
			$scope.graph = data;
		});
	}]);

function standardMenuItems(position) {
	var menuItems = [{"text": "Background Information", "href": "#/standard/background"},
                 {"text": "Select Trigger", "href": "#/standard/trigger"},
                 {"text": "Define Logic", "href": "#/standard/logic"},
                 {"text": "Choose Action", "href": "#/standard/action"},
                 {"text": "Review & Save", "href": "#/standard/save"}];
	menuItems[position].status = "disabled";
	return menuItems;
};