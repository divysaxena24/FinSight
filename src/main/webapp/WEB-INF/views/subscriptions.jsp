<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>FinSight | Subscriptions & Renewal Tracker</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <style>
        .subscriptions-container {
            max-width: 1200px;
            margin: 0 auto;
            padding: 2rem 1rem 4rem;
        }
        .subs-stats-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
            gap: 1.25rem;
            margin-bottom: 2rem;
        }
        .subs-stat-card {
            background: #ffffff;
            border: 1px solid var(--border);
            border-radius: var(--radius);
            padding: 1.25rem 1.5rem;
            box-shadow: var(--shadow-sm);
            display: flex;
            align-items: center;
            gap: 1.25rem;
        }
        .subs-stat-icon {
            width: 48px;
            height: 48px;
            border-radius: 12px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 1.5rem;
            flex-shrink: 0;
        }
        .subs-stat-val {
            font-size: 1.6rem;
            font-weight: 700;
            color: var(--dark);
            line-height: 1.2;
        }
        .subs-stat-label {
            font-size: 0.85rem;
            color: var(--text-muted);
            margin-top: 2px;
        }
        .subs-jump-nav {
            display: flex;
            gap: 0.75rem;
            flex-wrap: wrap;
            margin-bottom: 2rem;
            padding: 0.75rem 1rem;
            background: #ffffff;
            border: 1px solid var(--border);
            border-radius: 8px;
        }
        .subs-jump-link {
            text-decoration: none;
            color: var(--primary);
            font-weight: 600;
            font-size: 0.9rem;
            padding: 0.35rem 0.75rem;
            border-radius: 6px;
            background: #eff6ff;
            transition: all 0.2s ease;
        }
        .subs-jump-link:hover {
            background: #dbeafe;
        }
        .subs-catalog-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
            gap: 1.5rem;
        }
        .catalog-card {
            background: #ffffff;
            border: 2px solid var(--border);
            border-radius: var(--radius);
            padding: 1.5rem;
            box-shadow: var(--shadow-sm);
            display: flex;
            flex-direction: column;
            justify-content: space-between;
            transition: all 0.2s ease;
            position: relative;
        }
        .catalog-card:hover {
            border-color: #cbd5e1;
            box-shadow: var(--shadow);
        }
        .catalog-card.is-tracked {
            border-color: #3b82f6;
            background: linear-gradient(180deg, #ffffff 0%, #f0f7ff 100%);
            box-shadow: 0 4px 12px rgba(59, 130, 246, 0.12);
        }
        .subs-service-logo {
            width: 40px;
            height: 40px;
            border-radius: 10px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-weight: 800;
            font-size: 1.2rem;
            color: #ffffff;
            flex-shrink: 0;
            box-shadow: 0 2px 5px rgba(0,0,0,0.15);
        }
        .logo-netflix { background: #e50914; }
        .logo-amazon-prime { background: #00a8e1; }
        .logo-disney-hotstar { background: #0c111b; }
        .logo-spotify { background: #1db954; }
        .logo-youtube-premium { background: #ff0000; }
        .logo-chatgpt { background: #10a37f; }
        .logo-jiocinema { background: #e11b6f; }
        .logo-zee5 { background: #8230c6; }
        .logo-sonyliv { background: #222222; }
        .logo-apple-music { background: #fc3c44; }
        .logo-default { background: #475569; }

        .subs-tick-box {
            display: flex;
            align-items: center;
            gap: 8px;
            padding: 6px 12px;
            border-radius: 20px;
            font-size: 0.85rem;
            font-weight: 700;
            cursor: pointer;
            border: 1px solid var(--border);
            background: #f8fafc;
            color: #64748b;
            user-select: none;
            transition: all 0.2s ease;
        }
        .subs-tick-box.is-ticked {
            background: #dcfce7;
            border-color: #86efac;
            color: #15803d;
        }
        .subs-tick-box:hover {
            border-color: #94a3b8;
        }
        .subs-category-tag {
            display: inline-block;
            font-size: 0.72rem;
            font-weight: 700;
            text-transform: uppercase;
            letter-spacing: 0.5px;
            padding: 3px 8px;
            border-radius: 6px;
            background: #f1f5f9;
            color: #475569;
        }
        .subs-form-inline {
            background: #f8fafc;
            border: 1px dashed #cbd5e1;
            border-radius: 10px;
            padding: 1rem;
            margin-top: 1rem;
        }
        .subs-form-inline label {
            display: block;
            font-size: 0.8rem;
            font-weight: 700;
            color: #334155;
            margin-bottom: 0.35rem;
        }
        .subs-form-inline .form-control {
            width: 100%;
            padding: 0.5rem 0.65rem;
            font-size: 0.85rem;
            border-radius: 6px;
            border: 1px solid #cbd5e1;
            background: #ffffff;
            margin-bottom: 0.75rem;
        }
        .subs-meta-verified {
            font-size: 0.72rem;
            color: #94a3b8;
            margin-top: 0.5rem;
            display: flex;
            align-items: center;
            gap: 4px;
        }
    </style>
</head>
<body class="subscriptions-page">
    <nav class="navbar">
        <a href="${pageContext.request.contextPath}/dashboard" class="brand">
            💰 FinSight
        </a>
        <div class="nav-links" id="subscriptions-nav-links">
            <a href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
            <a href="${pageContext.request.contextPath}/accounts">My Accounts</a>
            <a href="${pageContext.request.contextPath}/banking?action=transfer">Transfer</a>
            <a href="${pageContext.request.contextPath}/banking?action=deposit">Deposit</a>
            <a href="${pageContext.request.contextPath}/banking?action=withdraw">Withdraw</a>
            <a href="${pageContext.request.contextPath}/viewTransaction">Transactions</a>
            <a href="${pageContext.request.contextPath}/budgets">Budgets</a>
            <a href="${pageContext.request.contextPath}/subscriptions" class="active" aria-current="page">Subscriptions</a>
            <a href="${pageContext.request.contextPath}/alerts">Alerts</a>
            <a href="${pageContext.request.contextPath}/health">Financial Health</a>
            <a href="${pageContext.request.contextPath}/statement-analysis">Statements</a>
            <a href="${pageContext.request.contextPath}/profile">Profile</a>
            <a href="${pageContext.request.contextPath}/logout" class="btn-logout">Logout</a>
        </div>
    </nav>

    <div class="container subscriptions-container">
        <!-- Page Header -->
        <div class="page-header" style="margin-bottom: 1.5rem;">
            <h1 class="page-title">&#128241; Subscriptions &amp; Renewal Tracker</h1>
            <p class="page-subtitle">Track your popular recurring services, stay alert of renewal deadlines, and never get charged unexpectedly</p>
        </div>

        <!-- Flash Messages -->
        <c:if test="${not empty sessionScope.successMessage}">
            <div class="alert alert-success" style="margin-bottom: 1.5rem;">
                <span><c:out value="${sessionScope.successMessage}"/></span>
            </div>
            <c:remove var="successMessage" scope="session"/>
        </c:if>

        <c:if test="${not empty sessionScope.errorMessage}">
            <div class="alert alert-danger" style="margin-bottom: 1.5rem;">
                <span><c:out value="${sessionScope.errorMessage}"/></span>
            </div>
            <c:remove var="errorMessage" scope="session"/>
        </c:if>

        <!-- Deadline Reminders Banner (30 days, 10 days, 7 days or less) -->
        <c:if test="${not empty subscriptionReminders}">
            <div style="margin-bottom: 2rem;">
                <c:forEach var="rem" items="${subscriptionReminders}">
                    <div class="alert" style="display: flex; align-items: center; justify-content: space-between; border-left: 6px solid ${rem.alertColor}; background: ${rem.alertBgColor}; color: ${rem.alertColor}; border: 1px solid ${rem.alertBorderColor}; padding: 14px 18px; border-radius: 10px; margin-bottom: 10px; box-shadow: 0 2px 6px rgba(0,0,0,0.05);">
                        <div style="display: flex; align-items: center; gap: 14px;">
                            <span style="font-size: 1.6rem;">
                                <c:choose>
                                    <c:when test="${rem.alertLevel == 'red'}">&#9888;&#65039;</c:when>
                                    <c:when test="${rem.alertLevel == 'orange'}">&#9200;</c:when>
                                    <c:otherwise>&#128197;</c:otherwise>
                                </c:choose>
                            </span>
                            <div>
                                <strong style="font-size: 0.95rem; display: block; margin-bottom: 2px;">
                                    <c:out value="${rem.displayName}"/> (<c:out value="${rem.planName}"/>) &bull; Next Renewal: <c:out value="${rem.renewalLabel}"/>
                                </strong>
                                <span style="font-size: 0.9rem; line-height: 1.4;">
                                    <c:out value="${rem.reminderMessage}"/>
                                </span>
                            </div>
                        </div>
                        <div style="display: flex; align-items: center; gap: 8px; flex-shrink: 0; margin-left: 16px;">
                            <span class="badge" style="background: ${rem.alertLevel == 'red' ? '#fee2e2' : (rem.alertLevel == 'orange' ? '#fef3c7' : '#dbeafe')}; color: ${rem.alertLevel == 'red' ? '#991b1b' : (rem.alertLevel == 'orange' ? '#92400e' : '#1e40af')}; font-weight: 700; padding: 6px 12px; border-radius: 20px; font-size: 0.85rem;">
                                <c:choose>
                                    <c:when test="${rem.daysRemaining == 0}">Due Today!</c:when>
                                    <c:when test="${rem.daysRemaining == 1}">Due Tomorrow</c:when>
                                    <c:otherwise>${rem.daysRemaining} days left</c:otherwise>
                                </c:choose>
                            </span>
                        </div>
                    </div>
                </c:forEach>
            </div>
        </c:if>

        <!-- Stats Overview Row -->
        <div class="subs-stats-grid">
            <div class="subs-stat-card">
                <div class="subs-stat-icon" style="background: #eff6ff; color: #2563eb;">&#128203;</div>
                <div>
                    <div class="subs-stat-val"><c:out value="${trackedCount != null ? trackedCount : 0}"/></div>
                    <div class="subs-stat-label">Active Tracked Subscriptions</div>
                </div>
            </div>
            <div class="subs-stat-card">
                <div class="subs-stat-icon" style="background: #fef3c7; color: #d97706;">&#9200;</div>
                <div>
                    <div class="subs-stat-val"><c:out value="${renewingWithin7Count != null ? renewingWithin7Count : 0}"/></div>
                    <div class="subs-stat-label">Renewing Within 7 Days</div>
                </div>
            </div>
            <div class="subs-stat-card">
                <div class="subs-stat-icon" style="background: #f0fdf4; color: #16a34a;">&#128161;</div>
                <div>
                    <div class="subs-stat-val">10</div>
                    <div class="subs-stat-label">Popular Verified Services</div>
                </div>
            </div>
        </div>

        <!-- Quick Jump Links (Only the 2 required sections) -->
        <div class="subs-jump-nav">
            <span style="font-size: 0.85rem; font-weight: 700; color: #64748b; align-self: center; margin-right: 0.5rem;">JUMP TO:</span>
            <a href="#catalogSection" class="subs-jump-link">&#11088; Top 10 Popular Subscriptions</a>
            <a href="#trackedSection" class="subs-jump-link">&#128203; My Tracked Subscriptions (<c:out value="${trackedCount != null ? trackedCount : 0}"/>)</a>
        </div>

        <!-- SECTION 1: Top 10 Popular Subscriptions in India -->
        <section id="catalogSection" style="margin-bottom: 3rem;">
            <div style="margin-bottom: 1.5rem;">
                <h2 style="font-size: 1.35rem; font-weight: 700; color: var(--dark); display: flex; align-items: center; gap: 0.5rem;">
                    &#11088; Top 10 Popular Subscriptions in India
                </h2>
                <p style="font-size: 0.9rem; color: var(--text-muted); margin-top: 0.25rem;">
                    Official plans and pricing in INR (₹). Tick any service to configure your plan and start date. Tracking is completely optional.
                </p>
            </div>

            <div class="subs-catalog-grid">
                <c:forEach var="card" items="${catalogCards}">
                    <div class="catalog-card ${card.tracked ? 'is-tracked' : ''}">
                        <div>
                            <!-- Card Header: Logo, Title, and Tick / Selection Checkbox -->
                            <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 0.75rem;">
                                <div style="display: flex; align-items: center; gap: 0.75rem;">
                                    <div class="subs-service-logo logo-${card.offer.code}">
                                        <c:choose>
                                            <c:when test="${card.offer.code == 'netflix'}">N</c:when>
                                            <c:when test="${card.offer.code == 'amazon-prime'}">P</c:when>
                                            <c:when test="${card.offer.code == 'disney-hotstar'}">H</c:when>
                                            <c:when test="${card.offer.code == 'spotify'}">S</c:when>
                                            <c:when test="${card.offer.code == 'youtube-premium'}">Y</c:when>
                                            <c:when test="${card.offer.code == 'chatgpt'}">G</c:when>
                                            <c:when test="${card.offer.code == 'jiocinema'}">J</c:when>
                                            <c:when test="${card.offer.code == 'zee5'}">Z</c:when>
                                            <c:when test="${card.offer.code == 'sonyliv'}">L</c:when>
                                            <c:when test="${card.offer.code == 'apple-music'}">A</c:when>
                                            <c:otherwise>&#9889;</c:otherwise>
                                        </c:choose>
                                    </div>
                                    <div>
                                        <div style="font-size: 1.15rem; font-weight: 700; color: var(--dark);">
                                            <c:out value="${card.offer.name}"/>
                                        </div>
                                        <span class="subs-category-tag"><c:out value="${card.offer.category}"/></span>
                                    </div>
                                </div>

                                <!-- Tick / Untick Interactive Toggle -->
                                <c:choose>
                                    <c:when test="${card.tracked}">
                                        <form action="${pageContext.request.contextPath}/subscriptions" method="post" style="margin: 0;">
                                            <input type="hidden" name="action" value="stop">
                                            <input type="hidden" name="subscriptionId" value="${card.userSubscription.id}">
                                            <input type="hidden" name="csrfToken" value="${csrfToken}">
                                            <button type="submit" class="subs-tick-box is-ticked" onclick="return confirm('Untick and stop tracking ${card.offer.name}?')" title="Click to untick">
                                                &#10003; Tracked
                                            </button>
                                        </form>
                                    </c:when>
                                    <c:otherwise>
                                        <div class="subs-tick-box" onclick="toggleTrackingForm('${card.offer.code}')" title="Click to track this service">
                                            &#9633; Track
                                        </div>
                                    </c:otherwise>
                                </c:choose>
                            </div>

                            <p style="font-size: 0.85rem; color: var(--text-muted); line-height: 1.45; margin-bottom: 0.75rem;">
                                <c:out value="${card.offer.summary}"/>
                            </p>

                            <!-- If Tracked: Display details & Next Renewal Countdown -->
                            <c:if test="${card.tracked}">
                                <div style="background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 0.9rem; margin-bottom: 0.75rem;">
                                    <div style="display: flex; justify-content: space-between; font-size: 0.85rem; margin-bottom: 0.35rem;">
                                        <span style="color: var(--text-muted);">Active Plan:</span>
                                        <strong style="color: var(--dark);"><c:out value="${card.userSubscription.planName}"/></strong>
                                    </div>
                                    <div style="display: flex; justify-content: space-between; font-size: 0.85rem; margin-bottom: 0.35rem;">
                                        <span style="color: var(--text-muted);">Cost:</span>
                                        <strong style="color: #2563eb;"><c:out value="${card.userSubscription.priceLabel}"/></strong>
                                    </div>
                                    <div style="display: flex; justify-content: space-between; font-size: 0.85rem; margin-bottom: 0.35rem;">
                                        <span style="color: var(--text-muted);">Subscribed On:</span>
                                        <span><c:out value="${card.userSubscription.startDate}"/></span>
                                    </div>
                                    <div style="display: flex; justify-content: space-between; font-size: 0.85rem; margin-bottom: 0.35rem;">
                                        <span style="color: var(--text-muted);">Next Renewal:</span>
                                        <strong style="color: #0f172a;"><c:out value="${card.userSubscription.renewalLabel}"/></strong>
                                    </div>

                                    <div style="margin-top: 0.5rem;">
                                        <span class="badge" style="background: ${card.userSubscription.alertLevel == 'red' ? '#fee2e2' : (card.userSubscription.alertLevel == 'orange' ? '#fef3c7' : '#dbeafe')}; color: ${card.userSubscription.alertLevel == 'red' ? '#991b1b' : (card.userSubscription.alertLevel == 'orange' ? '#92400e' : '#1e40af')}; font-weight: 700; padding: 5px 10px; border-radius: 6px; font-size: 0.8rem; display: inline-block;">
                                            <c:choose>
                                                <c:when test="${card.userSubscription.daysRemaining == 0}">Renews Today!</c:when>
                                                <c:when test="${card.userSubscription.daysRemaining == 1}">Renews Tomorrow</c:when>
                                                <c:otherwise>Renews in ${card.userSubscription.daysRemaining} days</c:otherwise>
                                            </c:choose>
                                        </span>
                                    </div>
                                </div>
                            </c:if>
                        </div>

                        <!-- Card Action / Form Area -->
                        <div style="margin-top: 0.75rem;">
                            <c:choose>
                                <c:when test="${card.tracked}">
                                    <div style="display: flex; gap: 8px;">
                                        <button type="button" class="btn btn-secondary btn-sm" style="flex: 1;" onclick="toggleTrackingForm('${card.offer.code}')">
                                            Edit Plan &amp; Date
                                        </button>
                                        <form action="${pageContext.request.contextPath}/subscriptions" method="post" style="margin: 0;">
                                            <input type="hidden" name="action" value="stop">
                                            <input type="hidden" name="subscriptionId" value="${card.userSubscription.id}">
                                            <input type="hidden" name="csrfToken" value="${csrfToken}">
                                            <button type="submit" class="btn btn-outline-danger btn-sm" onclick="return confirm('Untick and remove ${card.offer.name}?')">
                                                Untick
                                            </button>
                                        </form>
                                    </div>
                                </c:when>
                                <c:otherwise>
                                    <button type="button" class="btn btn-outline-primary btn-sm" style="width: 100%;" onclick="toggleTrackingForm('${card.offer.code}')">
                                        + Choose Plan &amp; Track
                                    </button>
                                </c:otherwise>
                            </c:choose>

                            <!-- Inline Modal / Form for Plan Selection & Date Picker -->
                            <div id="form-${card.offer.code}" class="subs-form-inline" style="display: none;">
                                <form action="${pageContext.request.contextPath}/subscriptions" method="post">
                                    <input type="hidden" name="action" value="track">
                                    <input type="hidden" name="catalogCode" value="${card.offer.code}">
                                    <input type="hidden" name="csrfToken" value="${csrfToken}">

                                    <div>
                                        <label>1. Select Your Plan *</label>
                                        <select name="planCode" class="form-control" required>
                                            <option value="">-- Choose a Plan (in ₹) --</option>
                                            <c:forEach var="plan" items="${card.offer.plans}">
                                                <option value="${plan.code}" ${card.tracked && plan.code == card.userSubscription.planCode ? 'selected' : ''}>
                                                    <c:out value="${plan.optionLabel}"/>
                                                </option>
                                            </c:forEach>
                                        </select>
                                    </div>

                                    <div>
                                        <label>2. Subscription Start Date *</label>
                                        <input type="date" name="startDate" max="${today}" value="${card.tracked ? card.userSubscription.startDate : today}" class="form-control" required>
                                        <small style="color: #64748b; font-size: 0.75rem; display: block; margin-top: -0.5rem; margin-bottom: 0.75rem;">
                                            Auto-calculates next renewal from billing cycle
                                        </small>
                                    </div>

                                    <div style="display: flex; gap: 8px;">
                                        <button type="submit" class="btn btn-primary btn-sm" style="flex: 1;">
                                            ${card.tracked ? 'Update Details' : 'Save & Track'}
                                        </button>
                                        <button type="button" class="btn btn-secondary btn-sm" onclick="toggleTrackingForm('${card.offer.code}')">
                                            Cancel
                                        </button>
                                    </div>
                                </form>
                            </div>

                            <div class="subs-meta-verified">
                                <span>&#128197;</span> Official India Pricing &bull; Verified Nov 2026
                            </div>
                        </div>
                    </div>
                </c:forEach>
            </div>
        </section>

        <!-- SECTION 2: My Tracked Subscriptions Overview Table -->
        <section id="trackedSection" style="margin-bottom: 3rem;">
            <div class="card" style="padding: 1.5rem; background: #fff; border: 1px solid var(--border); border-radius: var(--radius); box-shadow: var(--shadow-sm);">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.25rem; flex-wrap: wrap; gap: 8px;">
                    <div>
                        <h2 style="font-size: 1.25rem; font-weight: 700; color: var(--dark); display: flex; align-items: center; gap: 0.5rem;">
                            &#128203; My Tracked Subscriptions (<c:out value="${trackedCount != null ? trackedCount : 0}"/>)
                        </h2>
                        <p style="font-size: 0.85rem; color: var(--text-muted); margin-top: 0.25rem;">
                            Active subscriptions currently monitoring deadlines and generating dashboard alerts
                        </p>
                    </div>
                </div>

                <c:choose>
                    <c:when test="${empty trackedSubscriptions}">
                        <div style="text-align: center; padding: 2.5rem 1rem; background: #f8fafc; border-radius: 8px; border: 1px dashed var(--border);">
                            <div style="font-size: 2.5rem; margin-bottom: 0.75rem;">&#128241;</div>
                            <h3 style="font-size: 1.1rem; font-weight: 600; color: var(--dark); margin-bottom: 0.35rem;">No Subscriptions Tracked Yet</h3>
                            <p style="font-size: 0.85rem; color: var(--text-muted); max-width: 480px; margin: 0 auto 1.25rem;">
                                Tracking is optional! Click on any service above to tick it, choose your plan, and set your subscription date.
                            </p>
                            <a href="#catalogSection" class="btn btn-primary btn-sm">Browse 10 Popular Services</a>
                        </div>
                    </c:when>

                    <c:otherwise>
                        <div style="overflow-x: auto;">
                            <table class="subs-table">
                                <thead>
                                    <tr>
                                        <th>Service Name</th>
                                        <th>Plan</th>
                                        <th>Price &amp; Cycle</th>
                                        <th>Subscribed Date</th>
                                        <th>Next Renewal Deadline</th>
                                        <th>Status</th>
                                        <th style="text-align: right;">Action</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="sub" items="${trackedSubscriptions}">
                                        <tr>
                                            <td>
                                                <div style="display: flex; align-items: center; gap: 10px;">
                                                    <div class="subs-service-logo logo-${sub.catalogCode}" style="width: 32px; height: 32px; font-size: 0.95rem; border-radius: 6px;">
                                                        <c:choose>
                                                            <c:when test="${sub.catalogCode == 'netflix'}">N</c:when>
                                                            <c:when test="${sub.catalogCode == 'amazon-prime'}">P</c:when>
                                                            <c:when test="${sub.catalogCode == 'disney-hotstar'}">H</c:when>
                                                            <c:when test="${sub.catalogCode == 'spotify'}">S</c:when>
                                                            <c:when test="${sub.catalogCode == 'youtube-premium'}">Y</c:when>
                                                            <c:when test="${sub.catalogCode == 'chatgpt'}">G</c:when>
                                                            <c:when test="${sub.catalogCode == 'jiocinema'}">J</c:when>
                                                            <c:when test="${sub.catalogCode == 'zee5'}">Z</c:when>
                                                            <c:when test="${sub.catalogCode == 'sonyliv'}">L</c:when>
                                                            <c:when test="${sub.catalogCode == 'apple-music'}">A</c:when>
                                                            <c:otherwise>&#9889;</c:otherwise>
                                                        </c:choose>
                                                    </div>
                                                    <strong><c:out value="${sub.displayName}"/></strong>
                                                </div>
                                            </td>
                                            <td><c:out value="${sub.planName}"/></td>
                                            <td style="font-weight: 600; color: #2563eb;"><c:out value="${sub.priceLabel}"/></td>
                                            <td><c:out value="${sub.startDate}"/></td>
                                            <td>
                                                <strong style="color: #0f172a;"><c:out value="${sub.renewalLabel}"/></strong>
                                            </td>
                                            <td>
                                                <span class="badge" style="background: ${sub.alertLevel == 'red' ? '#fee2e2' : (sub.alertLevel == 'orange' ? '#fef3c7' : '#dbeafe')}; color: ${sub.alertLevel == 'red' ? '#991b1b' : (sub.alertLevel == 'orange' ? '#92400e' : '#1e40af')}; font-weight: 700; padding: 4px 8px; border-radius: 6px;">
                                                    <c:choose>
                                                        <c:when test="${sub.daysRemaining == 0}">Due Today!</c:when>
                                                        <c:when test="${sub.daysRemaining == 1}">Due Tomorrow</c:when>
                                                        <c:otherwise>${sub.daysRemaining} days left</c:otherwise>
                                                    </c:choose>
                                                </span>
                                            </td>
                                            <td style="text-align: right;">
                                                <form action="${pageContext.request.contextPath}/subscriptions" method="post" style="display: inline; margin: 0;">
                                                    <input type="hidden" name="action" value="stop">
                                                    <input type="hidden" name="subscriptionId" value="${sub.id}">
                                                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                                                    <button type="submit" class="btn btn-outline-danger btn-sm" onclick="return confirm('Untick and remove ${sub.displayName}?')">
                                                        Untick
                                                    </button>
                                                </form>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </section>
    </div>

    <script>
        function toggleTrackingForm(code) {
            var el = document.getElementById('form-' + code);
            if (el) {
                el.style.display = (el.style.display === 'none' || el.style.display === '') ? 'block' : 'none';
            }
        }
    </script>
</body>
</html>
