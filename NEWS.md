## v2.0.1 2022-11-04

### Stories
* [MODINREACH-320](https://issues.folio.org/browse/MODINREACH-320) - Fix missing property volumeDesignation from item contribution json payload

## v2.0.0 2022-08-17

### Stories
* [MODINREACH-281](https://issues.folio.org/browse/MODINREACH-281) - Store setting to indicate whether to look up pickup locations for INN-Reach item hold requests based on transaction pickupLocation
* [MODINREACH-282](https://issues.folio.org/browse/MODINREACH-282) - When the option is enabled in INN-Reach settings, look up pickup locations for FOLIO requests assigned to INN-Reach Item Hold transactions using the pickupLocation provided by the central server
* [MODINREACH-285](https://issues.folio.org/browse/MODINREACH-285) - Patron Hold Transactions Not Changed to RETURN_UNCIRCULATED State When Checked in AFTER the local FOLIO Request for the Item is Cancelled BEFORE The Item is Placed on the Hold Shelf
* [MODINREACH-287](https://issues.folio.org/browse/MODINREACH-287) - iNN-Reach Settings: Manage INN-Reach Paging Slip Template for Central Server
* [MODINREACH-288](https://issues.folio.org/browse/MODINREACH-288) - INN-Reach Transactions List Action Menu Item: INN-Reach paging slips
* [MODINREACH-291](https://issues.folio.org/browse/MODINREACH-291) - mod-inn-reach - folio-spring-base update - Morning Glory 2022 R2
* [MODINREACH-292](https://issues.folio.org/browse/MODINREACH-292) - The field "Author" on the Transaction Detail View in the "Item Information" accordion is not filled
* [MODINREACH-296](https://issues.folio.org/browse/MODINREACH-296) -  INN-Reach Transactions List Action Menu Item: INN-Reach paging slips. Add centralServerId
* [MODINREACH-297](https://issues.folio.org/browse/MODINREACH-297) - INN-Reach Settings: INN-Reach Paging Slip Templates for all Central Servers
* [MODINREACH-300](https://issues.folio.org/browse/MODINREACH-300) - Logging of requests/responses from the central server is not working


## v1.1.0 2022-04-28

Release primarily addresses Circulation flow and bug fixes. 

### Stories

* [MODINREACH-267](https://issues.folio.org/browse/MODINREACH-267) - INN-Reach Transaction Detail View Action Menu (Local hold) - Transfer hold to another item
* [MODINREACH-265](https://issues.folio.org/browse/MODINREACH-265) - INN-Reach Transaction Detail View Action Menu (Item hold) - Final check-in
* [MODINREACH-262](https://issues.folio.org/browse/MODINREACH-262) - INN-Reach Transaction Detail View Action Menu (Item hold) - Transfer hold to another item
* [MODINREACH-264](https://issues.folio.org/browse/MODINREACH-264) - INN-Reach Transaction Detail View Action Menu (Item hold) - Cancel hold
* [MODINREACH-246](https://issues.folio.org/browse/MODINREACH-246) - INN-Reach Circulation Flow Terminating Transaction Handling: Cancel Item Hold (Borrowing Site)
* [MODINREACH-269](https://issues.folio.org/browse/MODINREACH-269) - "Visible-patron-field-configuration" permissions don't work
* [MODINREACH-261](https://issues.folio.org/browse/MODINREACH-261) - Ensure that INN-Reach transaction state is persisted before Kafka events are consumed
* [MODINREACH-128](https://issues.folio.org/browse/MODINREACH-128) - FOLIO INN-Reach Integration
* [MODINREACH-239](https://issues.folio.org/browse/MODINREACH-239) - INN-Reach Staff Interface: INN-Reach Transaction Detail View Action Menu (Patron hold) - Return Item API
* [MODINREACH-247](https://issues.folio.org/browse/MODINREACH-247) - INN-Reach Circulation Flow Terminating Transaction Handling: Cancel Request (Borrowing Site)
* [MODINREACH-260](https://issues.folio.org/browse/MODINREACH-260) - Final Check In Fails at Borrowing Site if Transaction State is ITEM_RECEIVED
* [MODINREACH-259](https://issues.folio.org/browse/MODINREACH-259) - INN-Reach Settings: Create CRUD REST API Endpoints for Visible Patron Field Configuration
* [MODINREACH-251](https://issues.folio.org/browse/MODINREACH-251) - INN-Reach Circulation Flow Terminating Transaction Handling: Claims Returned (Borrowing Site)
* [MODINREACH-250](https://issues.folio.org/browse/MODINREACH-250) - INN-Reach Circulation Flow Terminating Transaction Handling: Patron Claims Returned (Owning Site)
* [MODINREACH-249](https://issues.folio.org/browse/MODINREACH-249) - INN-Reach Circulation Flow Terminating Transaction Handling: Local Checkout (Local Hold)
* [MODINREACH-248](https://issues.folio.org/browse/MODINREACH-248) - INN-Reach Circulation Flow Terminating Transaction Handling: Cancel Request (Owning Site)
* [MODINREACH-244](https://issues.folio.org/browse/MODINREACH-244) - INN-Reach Circulation Flow Terminating Transaction Handling: Final Item Check-in (Borrowing Site)
* [MODINREACH-177](https://issues.folio.org/browse/MODINREACH-177) - INN-Reach Record Contribution: Ignore Items and Bibs from Locations Not Associated with INN-Reach Local Agency Libraries
* [MODINREACH-158](https://issues.folio.org/browse/MODINREACH-158) - INN-Reach Circulation Flow Action: INN-Reach Transaction Detail View Action (Patron hold) - Cancel hold
* [MODINREACH-245](https://issues.folio.org/browse/MODINREACH-245) - INN-Reach Circulation Flow Terminating Transaction Handling: Cancel Item Hold (Owning Site)
* [MODINREACH-257](https://issues.folio.org/browse/MODINREACH-257) - Owning Site Sending Unnecessary Cancel Request Call to Central Server after Borrowing Site Cancel Request Received
* [MODINREACH-258](https://issues.folio.org/browse/MODINREACH-258) - SPIKE: Determine Why Instances are Being Contributed multiple times in a row following come circulation actions
* [MODINREACH-213](https://issues.folio.org/browse/MODINREACH-213) - Make User Field Used to Match "visiblePatronId" from D2IR Patron Verification Endpoint Payload Configurable
* [MODINREACH-241](https://issues.folio.org/browse/MODINREACH-241) - Update D2IR Local Endpoint (Circulation): Report Item Received to Owning Site for Item Hold to Support ITEM_HOLD and TRANSFERRED Transaction States
* [MODINREACH-256](https://issues.folio.org/browse/MODINREACH-256) - Update creation of holdings for INN-Reach Patron Hold items to include holdings source (FOLIO)
* [MODINREACH-236](https://issues.folio.org/browse/MODINREACH-236) - INN-Reach Circulation Flow Action: INN-Reach Transaction Detail View Action (Patron hold) - Check out to requesting patron
* [MODINREACH-149](https://issues.folio.org/browse/MODINREACH-149) - When Saving a Central Server Configuration, Validate That Libraries Are Only Assigned to a Single Agency Code
* [MODINREACH-217](https://issues.folio.org/browse/MODINREACH-217) - INN-Reach Transactions: Make transactions filterable by date comparison on metadata object dates and loanDueDate
* [MODINREACH-253](https://issues.folio.org/browse/MODINREACH-253) - INN-Reach Locations List Not Being Contributed Correctly
* [MODINREACH-252](https://issues.folio.org/browse/MODINREACH-252) - INN-Reach location should be optional for Location mapping
* [MODINREACH-161](https://issues.folio.org/browse/MODINREACH-161) - Inn-Reach Contribution Flow: support Cancelation API endpoint
* [MODINREACH-216](https://issues.folio.org/browse/MODINREACH-216) - Add Patron Name and Patron Type (INN-Reach) to Patron Hold Transaction Record
* [MODINREACH-238](https://issues.folio.org/browse/MODINREACH-238) - Update Receive Un-shipped Item API to Handle Receiving an Un-shipped Item with a Cancelled FOLIO Request
* [MODINREACH-240](https://issues.folio.org/browse/MODINREACH-240) - ASCII Newline Characters In Call Number Fields of Contributed Items Causing Errors on Central Server
* [MODINREACH-227](https://issues.folio.org/browse/MODINREACH-227) - Create API endpoint (GET) for location mappings
* [MODINREACH-233](https://issues.folio.org/browse/MODINREACH-233) - Update Receive Shipped Item API to Handle Receiving a Shipped Item with a Cancelled FOLIO Request
* [MODINREACH-6](https://issues.folio.org/browse/MODINREACH-6) - Record Contribution, Ongoing: Trigger contribution or update of Bibs and Items to INN-Reach central Server
* [MODINREACH-7](https://issues.folio.org/browse/MODINREACH-7) - Record Contribution, Ongoing: Trigger de-contribution of Bibs and Items from INN-Reach central Server
* [MODINREACH-231](https://issues.folio.org/browse/MODINREACH-231) - Checkout to Borrowing Site API Is Not Limiting Transaction Lookup by both item barcode and transaction state
* [MODINREACH-235](https://issues.folio.org/browse/MODINREACH-235) - Institutional Request Pickup Location Using Service Point Preference Default, not Request Preference Default Pickup Location
* [MODINREACH-234](https://issues.folio.org/browse/MODINREACH-234) - Local Hold Creation Failing When Item and Patron Agencies Do Not Match but Are On the Same Local Server
* [MODINREACH-162](https://issues.folio.org/browse/MODINREACH-162) - INN-Reach Circulation Flow: React to Loan Record Changes for INN-Reach Transactions - Local Hold: Check Out to Local Patron
* [MODINREACH-229](https://issues.folio.org/browse/MODINREACH-229) - Unable to Create Requests for Patron or Local Hold Transactions
* [MODINREACH-230](https://issues.folio.org/browse/MODINREACH-230) - Change Patron ID Handling for Verify Patron and Other D2IR APIs
* [MODINREACH-225](https://issues.folio.org/browse/MODINREACH-225) - INN-Reach Circulation Flow: Modify Reaction to Change in Request Record for INN-Reach Transaction: Cancel Request (Patron Hold)
* [MODINREACH-122](https://issues.folio.org/browse/MODINREACH-122) - INN-Reach Circulation Flow: React to Change in Request Record for INN-Reach Transaction: Transfer (Move) Request (Item Hold)
* [MODINREACH-165](https://issues.folio.org/browse/MODINREACH-165) - INN-Reach Circulation Flow: React to Loan Record Changes for INN-Reach Transactions - Patron Hold: Claim Returned
* [MODINREACH-176](https://issues.folio.org/browse/MODINREACH-176) - INN-Reach Circulation Flow: React to Changed Loan Record for INN-Reach Transaction: Item Recalled (Item Hold)
* [MODINREACH-226](https://issues.folio.org/browse/MODINREACH-226) - Enable logging of all responses and requests from the central server
* [MODINREACH-175](https://issues.folio.org/browse/MODINREACH-175) - INN-Reach Circulation Flow: React to Check-ins for INN-Reach Transactions - Patron Hold: Return Item Uncirculated
* [MODINREACH-173](https://issues.folio.org/browse/MODINREACH-173) - INN-Reach Circulation Flow: React to Change in Request Record for INN-Reach Transaction: Cancel Request (Item Hold)
* [MODINREACH-174](https://issues.folio.org/browse/MODINREACH-174) - INN-Reach Circulation Flow: React to Change in Request Record for INN-Reach Transaction: Cancel Request (Patron Hold)
* [MODINREACH-223](https://issues.folio.org/browse/MODINREACH-223) - Fix Initial record contribution fields mapping
* [MODINREACH-222](https://issues.folio.org/browse/MODINREACH-222) - Fix Initial record contribution - loading of source records
* [MODINREACH-218](https://issues.folio.org/browse/MODINREACH-218) - Transaction get request returns empty collection if offset >1
* [MODINREACH-169](https://issues.folio.org/browse/MODINREACH-169) - INN-Reach Circulation Flow: React to Loan Record Changes for INN-Reach Transactions - Item Hold: Final Check-in
* [MODINREACH-221](https://issues.folio.org/browse/MODINREACH-221) - Fix Initial record contribution event consuming
* [MODINREACH-220](https://issues.folio.org/browse/MODINREACH-220) - Remove "X-Request-Creation-Time" from all outgoing D2IR API requests
* [MODINREACH-219](https://issues.folio.org/browse/MODINREACH-219) - Remove "X-Request-Creation-Time" from list of required headers for 3rd-party D2IR API requests
* [MODINREACH-164](https://issues.folio.org/browse/MODINREACH-164) - INN-Reach Circulation Flow: React to Loan Record Changes for INN-Reach Transactions - Patron Hold: Return Item
* [MODINREACH-163](https://issues.folio.org/browse/MODINREACH-163) - INN-Reach Circulation Flow: React to Loan Record Changes for INN-Reach Transactions - Patron Hold: Borrower Renew Loan
* [MODINREACH-172](https://issues.folio.org/browse/MODINREACH-172) - D2IR Local Endpoint (Circulation): Claims Returned (Owning Site)
* [MODINREACH-159](https://issues.folio.org/browse/MODINREACH-159) - INN-Reach Circulation Flow: React to Loan Record Changes for INN-Reach Transactions - Patron Hold: Check Out to Patron
* [MODINREACH-214](https://issues.folio.org/browse/MODINREACH-214) - Owning Site Unable to Process Unshipped Item Message
* [MODINREACH-212](https://issues.folio.org/browse/MODINREACH-212) - Support circulation interface v13
* [MODINREACH-157](https://issues.folio.org/browse/MODINREACH-157) - INN-Reach Circulation Flow Action: Receive Un-shipped/Unannounced Item
* [MODINREACH-211](https://issues.folio.org/browse/MODINREACH-211) - Unable to Create Patron Hold Transactions
* [MODINREACH-206](https://issues.folio.org/browse/MODINREACH-206) - INN-Reach Item Hold Transactions Missing "title" Attribute
* [MODINREACH-184](https://issues.folio.org/browse/MODINREACH-184) - Add API to Update an INN-Reach Transaction Record
* [MODINREACH-56](https://issues.folio.org/browse/MODINREACH-56) - Spike: Record Contribution: Process contribution or update Jobs of Bibs to an INN-Reach central Server
* [MODINREACH-160](https://issues.folio.org/browse/MODINREACH-160) - Define missing internal permissions (modulePermissions) for API endpoints
* [MODINREACH-210](https://issues.folio.org/browse/MODINREACH-210) - Cannot create user due to lack of permissions
* [MODINREACH-199](https://issues.folio.org/browse/MODINREACH-199) - Include all Required Header Values on D2IR API Calls
* [MODINREACH-207](https://issues.folio.org/browse/MODINREACH-207) - Add new authorization header to D2IR API calls
* [MODINREACH-104](https://issues.folio.org/browse/MODINREACH-104) - D2IR Local Endpoint (Circulation): Owner Renew Item (Borrowing Site)
* [MODINREACH-201](https://issues.folio.org/browse/MODINREACH-201) - Adapt mod-inn-reach to the request schema changes
* [MODINREACH-102](https://issues.folio.org/browse/MODINREACH-102) - D2IR Local Endpoint (Circulation): Final Item Check-in (Borrowing Site)
* [MODINREACH-152](https://issues.folio.org/browse/MODINREACH-152) - Initial record contribution: Add modulePermissions required for calling other FOLIO modules
* [MODINREACH-205](https://issues.folio.org/browse/MODINREACH-205) - D2IR Local Endpoint (Circulation): Fix JSON Body for Borrower Renew Message for Item Hold (Owning Site)
* [MODINREACH-194](https://issues.folio.org/browse/MODINREACH-194) - D2IR Local Endpoint (Circulation): Check Out Unshipped Item to Borrowing Site When Unshipped Item Received Reported Received to Owning Site for Item Hold
* [MODINREACH-100](https://issues.folio.org/browse/MODINREACH-100) - D2IR Local Endpoint (Circulation): Borrower Renew Message for Item Hold (Owning Site)
* [MODINREACH-93](https://issues.folio.org/browse/MODINREACH-93) - D2IR Local Endpoint (Circulation): Create INN-Reach Local Hold - Create FOLIO Request
* [MODINREACH-89](https://issues.folio.org/browse/MODINREACH-89) - D2IR Local Endpoint (Circulation): Create INN-Reach Local Hold - Create Transaction
* [MODINREACH-182](https://issues.folio.org/browse/MODINREACH-182) - Enable INN-Reach Transaction Query by shippedItemBarcode and Transaction state
* [MODINREACH-99](https://issues.folio.org/browse/MODINREACH-99) - D2IR Local Endpoint (Circulation): Recall Item Message for Patron Hold

## v1.0.3 2022-05-20

### Bugs
* [MODINREACH-286](https://issues.folio.org/browse/MODINREACH-286) - Invalid mod-inn-reach-1.0.2 module descriptor

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
 
