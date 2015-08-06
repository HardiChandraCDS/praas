package com.github.rahulsom.praas

import com.github.rahulsom.swaggydoc.SwaggyList
import com.github.rahulsom.swaggydoc.SwaggyShow
import com.wordnik.swagger.annotations.*
import grails.converters.JSON
import grails.converters.XML
import grails.plugin.springsecurity.annotation.Secured
import grails.transaction.Transactional
import org.grails.plugins.metrics.groovy.Timed
import org.h2.tools.Csv
import org.hibernate.StatelessSession
import org.hibernate.Transaction

import java.sql.ResultSet
import java.text.SimpleDateFormat

import static org.springframework.http.HttpStatus.NOT_FOUND

@Transactional(readOnly = true)
@Secured('ROLE_USER')
@Api(value = "Provider", description = "Data on Providers",
        produces = 'application/json,application/hal+json,application/xml,text/html',
        consumes = 'application/json,application/xml,application/x-www-form-urlencoded'
)
class ProviderController {

    static allowedMethods = [save: "POST",]
    def providerService

    @SwaggyList
    @Timed(name = 'providersearch')
    def index() {
        params.max = Math.min(params.int('max') ?: 10, 100)
        if (params.q) {
            def search = Provider.search(params.q, params)
            respond search.searchResults.collect {Provider.get(it.id)}, model: [providerInstanceCount: search.total]
        } else {
            respond Provider.list(params), model: [providerInstanceCount: Provider.count()]
        }
    }

    @SwaggyShow
    @Timed(name = 'providershow')
    def show() {
        respond Provider.get(params.id)
    }

    @Secured('ROLE_ADMIN')
    @ApiOperation(value = "Save LOINC Codes", response = Void)
    @ApiResponses([
            @ApiResponse(code = 422, message = 'Bad Entity Received'),
    ])
    @ApiImplicitParams([
            @ApiImplicitParam(name = 'file', paramType = 'form', required = true, dataType = 'string',
                    value = "CSV File from downloaded zip. E.g. '/Users/rsom/Downloads/npidata_small.csv'"),
    ])
    def save() {
        providerService.loadData(params.file)

        withFormat {
            html {
                render("Complete!")
            }
            json {
                def retval = [status: 'Complete']
                render retval as JSON
            }
            xml {
                def retval = [status: 'Complete']
                render retval as XML
            }
        }
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'provider.label', default: 'Provider'), params.id])
                redirect action: "index", method: "GET"
            }
            '*' { render status: NOT_FOUND }
        }
    }
}
