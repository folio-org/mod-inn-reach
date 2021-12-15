## v1.0.2 2021-12-15

Resolve Log4Shell security issue ([CVE-2021-44228](https://nvd.nist.gov/vuln/detail/CVE-2021-44228) and [CVE-2021-45046](https://nvd.nist.gov/vuln/detail/CVE-2021-45046))

### Stories
* [MODINREACH-203](https://issues.folio.org/browse/MODINREACH-203) - Log4j vulnerability verification and correction

## v1.0.1 2021-10-08

Release primarily addresses Initial Contribution issues found during testing. 

### Stories

* [MODINREACH-145](https://issues.folio.org/browse/MODINREACH-145) - Fix record contribution integration errors
* [MODINREACH-140](https://issues.folio.org/browse/MODINREACH-140) - Update Iteration Job status object to reflect changes in mod-inventory-storage
* [MODINREACH-83](https://issues.folio.org/browse/MODINREACH-83) - D2IR Local Endpoint (Circulation): Create INN-Reach Item Hold - Create Transaction
* [MODINREACH-113](https://issues.folio.org/browse/MODINREACH-113) - D2IR Local Endpoint (Record Contribution): Get Bib Record

## v1.0.0 2021-09-24

The primary focus of this initial release was to support configuration settings for Inn-Reach integration and 
implement Initial Contribution process.

### Stories
 * [MODINREACH-134](https://issues.folio.org/browse/MODINREACH-134) - Store Central Server Code in Backend Model for Base INN-Reach Central Server Configuration
 * [MODINREACH-129](https://issues.folio.org/browse/MODINREACH-129) - Adjust MARC bib transformation to be Inn-reach compliant
 * [MODINREACH-124](https://issues.folio.org/browse/MODINREACH-124) - Populate Auditable entity data from Folio execution context
 * [MODINREACH-123](https://issues.folio.org/browse/MODINREACH-123) - INN-Reach Record Contribution: Requirements for determining itemCircStatus of contributed items
 * [MODINREACH-121](https://issues.folio.org/browse/MODINREACH-121) - Record Contribution: CRUD for job execution status tracking and basic statistics
 * [MODINREACH-120](https://issues.folio.org/browse/MODINREACH-120) - Record Contribution: Data model for job execution status tracking and basic statistics
 * [MODINREACH-119](https://issues.folio.org/browse/MODINREACH-119) - Record Contribution: Provide API endpoint to start Initial contribution process
 * [MODINREACH-118](https://issues.folio.org/browse/MODINREACH-118) - Record Contribution: Manage internal System user to interact with other modules during Kafka event processing
 * [MODINREACH-117](https://issues.folio.org/browse/MODINREACH-117) - Record Contribution: Introduce Kafka client to accept Instance events related to Initial contribution from mod-inventory-storage
 * [MODINREACH-116](https://issues.folio.org/browse/MODINREACH-116) - Circulation status mapping GET Error 500
 * [MODINREACH-115](https://issues.folio.org/browse/MODINREACH-115) - Add Id to Item Contribution Options Configuration
 * [MODINREACH-112](https://issues.folio.org/browse/MODINREACH-112) - Add PUT for material types collection mappings
 * [MODINREACH-111](https://issues.folio.org/browse/MODINREACH-111) - Apply "ON DELETE CASCADE" rule to all existing foreign key references where it's missing
 * [MODINREACH-110](https://issues.folio.org/browse/MODINREACH-110) - Not all central servers can be removed
 * [MODINREACH-109](https://issues.folio.org/browse/MODINREACH-109) - Simplify Record Contribution Criteria Configuration data model
 * [MODINREACH-101](https://issues.folio.org/browse/MODINREACH-101) - D2IR Local Endpoint (Circulation): Act on Borrower Renew Message for Item Hold (Owning Site)
 * [MODINREACH-94](https://issues.folio.org/browse/MODINREACH-94) - INN-Reach Central server Configuration: Add metadata
 * [MODINREACH-84](https://issues.folio.org/browse/MODINREACH-84) - Create Backend API Endpoint to GET a INN-Reach Central Item Types
 * [MODINREACH-82](https://issues.folio.org/browse/MODINREACH-82) - INN-Reach Record Contribution: Create CRUD REST API Endpoints for Data Model to Store Item Contribution Options Configuration
 * [MODINREACH-81](https://issues.folio.org/browse/MODINREACH-81) - INN-Reach Record Contribution: Create Data Model to Store Item Contribution Options Configuration
 * [MODINREACH-77](https://issues.folio.org/browse/MODINREACH-77) - INN-Reach Circulation Settings: Create API Endpoints to CRUD Mapping of Central Patron Types to Specific FOLIO User Barcodes
 * [MODINREACH-76](https://issues.folio.org/browse/MODINREACH-76) - INN-Reach Circulation Settings: Create Model to Store Mapping of Central Patron Types to Specific FOLIO User Barcodes
 * [MODINREACH-75](https://issues.folio.org/browse/MODINREACH-75) - Central server Configuration POST 400 error
 * [MODINREACH-74](https://issues.folio.org/browse/MODINREACH-74) - Central server Configuration POST 500 error
 * [MODINREACH-73](https://issues.folio.org/browse/MODINREACH-73) - Central server Configuration PUT 406 error
 * [MODINREACH-71](https://issues.folio.org/browse/MODINREACH-71) - INN-Reach Circulation Settings: Create Endpoints to CRUD User Custom Field to Local Agency Code Mapping
 * [MODINREACH-70](https://issues.folio.org/browse/MODINREACH-70) - INN-Reach Circulation Settings: Create Model to Store User Custom Field to Local Agency Code Mapping
 * [MODINREACH-69](https://issues.folio.org/browse/MODINREACH-69) - Add definition of /inn-reach/central-servers endpoints to OpenAPI spec of the module
 * [MODINREACH-68](https://issues.folio.org/browse/MODINREACH-68) - Key/Secret authentication REST endpoint
 * [MODINREACH-67](https://issues.folio.org/browse/MODINREACH-67) - Incorrect behavior of CentralServer REST API [POST, PUT]
 * [MODINREACH-66](https://issues.folio.org/browse/MODINREACH-66) - Proposal to adjust permissions and interface namespaces
 * [MODINREACH-65](https://issues.folio.org/browse/MODINREACH-65) - Create CRUD API(s) for Data Model to Store a Mapping of INN-Reach Agencies, Local Servers, and/or central servers to FOLIO Locations for Use with "Virtual Item Records" in FOLIO Inventory
 * [MODINREACH-64](https://issues.folio.org/browse/MODINREACH-64) - INN-Reach Circulation Settings: Create CRUD APIs for Local FOLIO Patron Group to INN-Reach Central Patron Type
 * [MODINREACH-63](https://issues.folio.org/browse/MODINREACH-63) - INN-Reach Circulation Settings: Create Data Model to Store FOLIO Patron Group to INN-Reach Central Patron Type Mapping
 * [MODINREACH-62](https://issues.folio.org/browse/MODINREACH-62) - INN-Reach Circulation Settings: Create CRUD APIs for Central Item Type to FOLIO Material Type Configuration for Patron Holds ("Virtual" item material type)
 * [MODINREACH-61](https://issues.folio.org/browse/MODINREACH-61) - INN-Reach Circulation Settings: Central Item Type to FOLIO Material Type Configuration for Patron Holds ("Virtual" item material type)
 * [MODINREACH-59](https://issues.folio.org/browse/MODINREACH-59) - Record Contribution: Process contribution or update Jobs of Items to an INN-Reach central Server
 * [MODINREACH-58](https://issues.folio.org/browse/MODINREACH-58) - Configure scratch environment and deploy mod-inn-reach module
 * [MODINREACH-55](https://issues.folio.org/browse/MODINREACH-55) - Add Metadata Object to all mod-inn-reach data models
 * [MODINREACH-54](https://issues.folio.org/browse/MODINREACH-54) - Align common backend module files with Folio standards
 * [MODINREACH-53](https://issues.folio.org/browse/MODINREACH-53) - Record Contribution: Trigger batch contribution or update of Items to INN-Reach central Server via API
 * [MODINREACH-51](https://issues.folio.org/browse/MODINREACH-51) - Record Contribution: Process contribution of Bibs to an INN-Reach central Server
 * [MODINREACH-50](https://issues.folio.org/browse/MODINREACH-50) - Record Contribution Settings: API to CRUD Data Model to Store Contributed MARC Transformation Options Settings for INN-Reach Central Servers
 * [MODINREACH-49](https://issues.folio.org/browse/MODINREACH-49) - Record Contribution Settings: Create a Data Model to Store Contributed MARC Transformation Options Settings for INN-Reach Central Servers
 * [MODINREACH-48](https://issues.folio.org/browse/MODINREACH-48) - Record Contribution: Retrieve and Transform MARC-backed Inventory Instance for Contribution to Central Server
 * [MODINREACH-47](https://issues.folio.org/browse/MODINREACH-47) - System Configuration: Trigger (Re-)submission of All Mapped Locations to the Central Server
 * [MODINREACH-46](https://issues.folio.org/browse/MODINREACH-46) - System Configuration: Submit Mapped Locations to the Central Server
 * [MODINREACH-45](https://issues.folio.org/browse/MODINREACH-45) - Create Backend API Endpoint to CRUD a Mapping of FOLIO Material Types to INN-Reach Central Item Types
 * [MODINREACH-44](https://issues.folio.org/browse/MODINREACH-44) - Create a Data Model to Store a Mapping of FOLIO Material Types to INN-Reach Central Item Types
 * [MODINREACH-43](https://issues.folio.org/browse/MODINREACH-43) - Create API Endpoint(s) to CRUD Record Contribution Criteria Configuration
 * [MODINREACH-42](https://issues.folio.org/browse/MODINREACH-42) - Create Record Contribution Criteria Configuration Model
 * [MODINREACH-41](https://issues.folio.org/browse/MODINREACH-41) - Provide API to Store an API Key and Secret Pair for Use by Associated INN-Reach Central Servers
 * [MODINREACH-15](https://issues.folio.org/browse/MODINREACH-15) - Check INN Reach access token lifecycle behavior.
 * [MODINREACH-39](https://issues.folio.org/browse/MODINREACH-39) - Create API Endpoint to Retrieve a Mapping of FOLIO Tenant Locations, locationUnits, and Service Points to INN-Reach Compatible Location Codes
 * [MODINREACH-38](https://issues.folio.org/browse/MODINREACH-38) - Create API Endpoint to Update a Mapping of FOLIO Tenant Locations, locationUnits, and Service Points to INN-Reach Compatible Location Codes
 * [MODINREACH-37](https://issues.folio.org/browse/MODINREACH-37) - Create API Endpoint to CRUD a Mapping of FOLIO Tenant Libraries and Shelving Locations to INN-Reach Compatible Location Codes
 * [MODINREACH-36](https://issues.folio.org/browse/MODINREACH-36) - Reference Data: INN-Reach Compatible Locations CRUD API
 * [MODINREACH-35](https://issues.folio.org/browse/MODINREACH-35) - Create API Endpoint to CRUD Base INN-Reach Central Server Configurations
 * [MODINREACH-34](https://issues.folio.org/browse/MODINREACH-34) - Create Backend Model for Base INN-Reach Central Server Configuration
 * [MODINREACH-17](https://issues.folio.org/browse/MODINREACH-17) - Reference Data: INN-Reach Compatible Locations Model
 * [MODINREACH-13](https://issues.folio.org/browse/MODINREACH-13) - INN-Reach Circulation Settings: Create a Data Model Store a Mapping of FOLIO Patron Groups to INN-Reach Central Patron Types
 * [MODINREACH-12](https://issues.folio.org/browse/MODINREACH-12) - Create a Data Model to Store a Mapping of INN-Reach Agencies, Local Servers, and/or central servers to FOLIO Locations for Use with "Virtual Item Records" in FOLIO Inventory
 * [MODINREACH-11](https://issues.folio.org/browse/MODINREACH-11) - Create a Data Model or Models to Store a Mapping of FOLIO Tenant Libraries and Shelving Locations to INN-Reach Compatible Location Codes
 * [MODINREACH-9](https://issues.folio.org/browse/MODINREACH-9) - Store INN-Reach/D2IR API Oauth2 Keys and Secrets Securely
 * [MODINREACH-8](https://issues.folio.org/browse/MODINREACH-8) - Define Data Model for INN-Reach Transactions (All Types)
 * [MODINREACH-4](https://issues.folio.org/browse/MODINREACH-4) - INN-Reach: Create Local Instance, Holdings, and Item Records for Patron Hold
 * [MODINREACH-1](https://issues.folio.org/browse/MODINREACH-1) - Store an API Key and Secret Pair for Use by Associated INN-Reach Central Servers
 