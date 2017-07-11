<%@ page contentType="text/html;charset=UTF-8"%>
        <!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <title>Blocks</title>
    <meta name="description" content="description">
    <meta name="author" content="Filip Grochowski Emil Wesołowski">
    <meta name="keyword" content="keywords">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    %{--<link rel="shortcut icon" href="${resource(dir:'images',file:'blocks_favicon.ico')}" type="image/x-icon" />--}%
    <asset:stylesheet src="application.css" />
    <asset:javascript src="application.js" />
</head>
<body>
<div class="container-fluid">
    <div id="page-login" class="row">
        <div
                class="col-xs-12 col-md-4 col-md-offset-4 col-sm-6 col-sm-offset-3">
            <div class="text-right"></div>
            <div class="box">
                <div class="box-content col-lg-12">
                    <g:layoutBody />
                </div>
            </div>
        </div>
    </div>
</div>
</body>
</html>