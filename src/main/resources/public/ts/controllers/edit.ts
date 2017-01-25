import { ng, sniplets } from 'entcore/entcore';
import { template, idiom } from 'entcore/entcore';
import { Website, Cell, Page, Folders, Media, Rows, Blocks, Block } from '../model';
import { _ } from 'entcore/libs/underscore/underscore';

export let edit = ng.controller('EditController', [
    '$scope', 'model', 'route', '$route', '$location', function ($scope, model, route, $route, $location) {
        let params = $route.current.params;
        let findPage = async (): Promise<void> => {
            if (params.pageId) {
                let websites = await Folders.websites();
                let website: Website = websites.find((w) => w._id === params.siteId);
                $scope.website = website;
                $scope.page = website.pages.matchingPath(params.pageId, website);
                let page: Page = $scope.page;
                page.rows.forEach((r) => {
                    $scope.display.guideRows.push($scope.display.guideRows.length);
                    $scope.display.guideRows.push($scope.display.guideRows.length);
                    $scope.display.guideRows.push($scope.display.guideRows.length);
                });
                $scope.page.addFillerRow();
                $scope.page.applySASS();
            }
            else {
                let websites = await Folders.websites();
                let website: Website = websites.find((w) => w._id === params.siteId);
                $scope.website = website;
                $scope.page = website.pages.landingPage(website);
                let page: Page = $scope.page;
                page.rows.forEach((r) => {
                    $scope.display.guideRows.push($scope.display.guideRows.length);
                    $scope.display.guideRows.push($scope.display.guideRows.length);
                    $scope.display.guideRows.push($scope.display.guideRows.length);
                });
                $scope.page.addFillerRow();
                $scope.page.applySASS();
            }

            $scope.websites = await Folders.websites();
            await Blocks.sync();
            $scope.blocks = Blocks;
            $scope.$apply();
        };

        model.on('route-changed', () => {
            params = $route.current.params;
            findPage();
        });
        findPage();

        template.open('view/grid', 'view/grid');
        template.open('editor/grid', 'editor/grid');

        Rows.eventer.on('add-row', () => {
            $scope.display.guideRows.push($scope.display.guideRows.length);
            $scope.display.guideRows.push($scope.display.guideRows.length);
            $scope.display.guideRows.push($scope.display.guideRows.length);
            $scope.$apply();
        })

        $scope.media = [
            { type: 'sound' },
            { type: 'video' },
            { type: 'text' },
            { type: 'image' }
        ];

        $scope.snipletsSources = sniplets.sniplets.map((s) => ({ 
                type: 'sniplet', 
                source: { application: s.application, template: s.template, title: s.sniplet.title } 
            })
        );

        $scope.publicSnipletsSources = _
            .filter(sniplets.sniplets, (s) => s.sniplet.public)
            .map((s) => ({
                type: 'sniplet',
                source: { application: s.application, template: s.template, title: s.sniplet.title }
            })
        );

        $scope.searchBlocks = (item: Block) => {
            return !$scope.display.searchBlocks || idiom.removeAccents(item.keywords.toLowerCase()).indexOf(
                idiom.removeAccents($scope.display.searchBlocks).toLowerCase()
            ) !== -1;
        };

        $scope.cellContent = (cell: Cell, content) => {
            cell.source(content);
            $scope.page.addFillerRow();
        };

        $scope.dropContent = (row, cell, $item) => {
            cell.source($item);
            row.page.addFillerRow();
            row.page.trigger('save');
        };

        $scope.removePage = () => {
            $scope.display.data.remove();
            $scope.lightbox('confirmRemovePage');
        };

        $scope.addPage = async () => {
            $scope.display.currentTemplate = undefined;
            await $scope.website.useNewPage();
            $scope.$apply();
        };

        $scope.preview = () => {
            if ($scope.website.visibility === 'PUBLIC') {
                if (params.pageId) {
                    window.location.href = '/pages/p/website#/preview/' + params.siteId + '/' + params.pageId;
                }
                else {
                    window.location.href = '/pages/p/website#/preview/' + params.siteId;
                }
            }
            else {
                if (params.pageId) {
                    $location.path('/preview/' + params.siteId + '/' + params.pageId)
                }
                else {
                    $location.path('/preview/' + params.siteId);
                }
            }
        };

        $scope.applySASS = (page: Page) => {
            page.applySASS();
        };

        $scope.closeManagePages = () => {
            $scope.lightbox('managePages');
            $scope.website.newPage = undefined;
            $scope.website.showStyle = undefined;
        };

        $scope.closeCellTitle = () => {
            $scope.lightbox('setCellTitle');
        }
}])