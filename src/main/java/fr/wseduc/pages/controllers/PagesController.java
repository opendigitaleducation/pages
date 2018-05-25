/*
 * Copyright © "Open Digital Education" (SAS “WebServices pour l’Education”), 2014
 *
 * This program is published by "Open Digital Education" (SAS “WebServices pour l’Education”).
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https: //opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package fr.wseduc.pages.controllers;

import fr.wseduc.bus.BusAddress;
import fr.wseduc.pages.Pages;
import fr.wseduc.pages.filters.PageReadFilter;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;

import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.mongodb.MongoDbControllerHelper;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.vertx.java.core.http.RouteMatcher;


import java.util.List;
import java.util.Map;

import static org.entcore.common.bus.BusResponseHandler.busResponseHandler;

public class PagesController extends MongoDbControllerHelper {

	private EventStore eventStore;
	private enum PagesEvent { ACCESS }

	@Override
	public void init(Vertx vertx, JsonObject config, RouteMatcher rm,
					 Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
		super.init(vertx, config, rm, securedActions);
		eventStore = EventStoreFactory.getFactory().getEventStore(Pages.class.getSimpleName());
	}

	public PagesController() {
		super("pages");
	}

	@Get("")
	@ApiDoc("Get page view")
	@SecuredAction("pages.view")
	public void view(HttpServerRequest request) {
		renderView(request);
		eventStore.createAndStoreEvent(PagesEvent.ACCESS.name(), request);
	}

	@Get("/p/website")
	public void websiteView(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new io.vertx.core.Handler<UserInfos>() {
			@Override
			public void handle(UserInfos user) {
				JsonObject context = new JsonObject().put("notLoggedIn", user == null);
				renderView(request, context, "website.html", null);
			}
		});
	}

	@Override
	@Get("/list/:filter")
	@ApiDoc("List all authorized pages.")
	@SecuredAction("pages.list")
	public void list(HttpServerRequest request) {
		super.list(request);
	}

	@Post("")
	@ApiDoc("Add page.")
	@SecuredAction("pages.add")
	public void add(HttpServerRequest request) {
		create(request);
	}

	@Post("/p")
	@ApiDoc("Add page.")
	@SecuredAction("pages.add.public")
	public void addPublic(HttpServerRequest request) {
		create(request);
	}

	@Get("/:id")
	@ApiDoc("Get page by id.")
	@ResourceFilter(PageReadFilter.class)
	@SecuredAction(value = "page.read", type = ActionType.RESOURCE)
	public void get(HttpServerRequest request) {
		retrieve(request);
	}

	@Override
	@Put("/:id")
	@ApiDoc("Update page by id.")
	@SecuredAction(value = "page.contrib", type = ActionType.RESOURCE)
	public void update(HttpServerRequest request) {
		super.update(request);
	}

	@Override
	@Delete("/:id")
	@ApiDoc("Delete page by id.")
	@SecuredAction(value = "page.manager", type = ActionType.RESOURCE)
	public void delete(HttpServerRequest request) {
		super.delete(request);
	}

	@Get("/pub/:id")
	@ApiDoc("Get public page by id.")
	public void getPublic(HttpServerRequest request) {
		retrieve(request);
	}

	@Get("/pub/list/:filter")
	@ApiDoc("List public pages.")
	public void listPublic(HttpServerRequest request) {
		list(request);
	}

	@Get("/share/json/:id")
	@ApiDoc("Get share list.")
	@SecuredAction(value = "page.manager", type = ActionType.RESOURCE)
	public void share(HttpServerRequest request) {
		shareJson(request, false);
	}

	@Put("/share/json/:id")
	@ApiDoc("Share a page.")
	@SecuredAction(value = "page.manager", type = ActionType.RESOURCE)
	public void shareSubmit(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					final String id = request.params().get("id");
					if(id == null || id.trim().isEmpty()) {
						badRequest(request, "invalid.id");
						return;
					}

					JsonObject params = new JsonObject()
							.put("uri", "/userbook/annuaire#" + user.getUserId() + "#" + user.getType())
							.put("username", user.getUsername())
							.put("pageUri", "/pages#/website/" + id);
					params.put("resourceUri", params.getString("pageUri"));

					shareJsonSubmit(request, "pages.shared", false, params, "title");
				}
			}
		});
	}

	@Put("/share/resource/:id")
	@ApiDoc("Share a page.")
	@SecuredAction(value = "page.manager", type = ActionType.RESOURCE)
	public void shareResource(final HttpServerRequest request) {
		UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
			@Override
			public void handle(final UserInfos user) {
				if (user != null) {
					final String id = request.params().get("id");
					if(id == null || id.trim().isEmpty()) {
						badRequest(request, "invalid.id");
						return;
					}

					JsonObject params = new JsonObject()
							.put("uri", "/userbook/annuaire#" + user.getUserId() + "#" + user.getType())
							.put("username", user.getUsername())
							.put("pageUri", "/pages#/website/" + id);
					params.put("resourceUri", params.getString("pageUri"));

					shareResource(request, "pages.shared", false, params, "title");
				}
			}
		});
	}

	@Override
	@Put("/share/remove/:id")
	@ApiDoc("Remove share.")
	@SecuredAction(value = "page.manager", type = ActionType.RESOURCE)
	public void removeShare(HttpServerRequest request) {
		super.removeShare(request, false);
	}

	@BusAddress("pages")
	public void busApi(Message<JsonObject> message) {
		String action = message.body().getString("action", "");
		final String pageId = message.body().getString("pageId");
		switch (action) {
			case "create" :
				UserInfos user = UserUtils.sessionToUserInfos(message.body().getJsonObject("user"));
				JsonObject page = message.body().getJsonObject("page");
				crudService.create(page, user, busResponseHandler(message));
				break;
			case "share" :
				String userId = message.body().getString("userId");
				String groupId = message.body().getString("groupId");
				List<String> actions = message.body().getJsonArray("actions").getList();
				shareService.groupShare(userId, groupId, pageId, actions, busResponseHandler(message));
				break;
			case "delete" :
				crudService.delete(pageId, busResponseHandler(message));
				break;
			case "get" :
				crudService.retrieve(pageId, busResponseHandler(message));
				break;
			case "update" :
				crudService.update(pageId, message.body().getJsonObject("page"), busResponseHandler(message));
				break;
			default:
				message.reply(new JsonObject().put("status", "error")
						.put("message", "invalid.action"));
		}
	}

}
