<template>
  <div style="padding:10px 30px">
    <Form
      :model="formItem"
      :label-width="100"
      @submit.native.prevent
      :rules="rules"
      ref="form"
    >
      <FormItem label="State" prop="state">
        <createSelect
          v-model="formItem.state"
          :canCreate="canCreateState"
          @on-change="stateChanged"
          :disabled="false"
        />
      </FormItem>
      <FormItem label="Action" prop="action">
        <customCascader
          @on-visible-change="cascaderVisableChange"
          :data="actions.system"
          v-model="formItem.action"
          placeholder="Select"
          @on-change="actionChanged"
          @showTooltip="showTooltip"
        ></customCascader>
      </FormItem>
      <FormItem label="Selected Query" prop="selected_query">
        <draggableList v-model="formItem.selected_query" @on-change="selectChanged"/>
      </FormItem>
      <FormItem label="Selected Results" prop="selected_result">
        <draggableList v-model="formItem.selected_result" @on-change="selectChanged"/>
      </FormItem>
      <FormItem label="Response" prop="response">
        <Input v-model="formItem.response" @on-blur="responseChanged" type="textarea" :rows="7" />
      </FormItem>
      <!--      <FormItem style="margin-bottom:0px">-->
      <!--        <Button type="default" @click="submit">Submit</Button>-->
      <!--        <Checkbox v-model="formItem.sendAnother" style="margin-left:20px">Send another message</Checkbox>-->
      <!--        <br />-->
      <!--        <a @click="finishConversation" v-if="role==='cus'">Finish the conversation>></a>-->
      <!--      </FormItem>-->
    </Form>
  </div>
</template>
<script>
import customCascader from '../../components/customCascader'
import helpTooltip from '../../components/helpTooltip'
import draggableList from '../../components/draggableList'
import createSelect from '../../components/createSelect'

const calAllow = (arr, actionNames, actionNameIndex) => {
  if (!actionNames[actionNameIndex]) return false
  let a = arr.filter(value => {
    return actionNames[actionNameIndex] === value.name
  })
  if (!a[0]) return false
  if (a[0].children) {
    return calAllow(a[0].children, actionNames, actionNameIndex + 1)
  } else { return a[0] && a[0].allowEmptySearch === true }
}

export default {
  components: {
    helpTooltip,
    draggableList,
    customCascader,
    createSelect
  },
  props: {
    conversationId: {
    },
    height: {
      type: String
    },
    role: {
      type: String
    },
    finish: {
      type: Function // ???????????????????????????????????????
    },
    actions: {}, // ??????action
    // currentState: {}, // ??????????????????state???????????????????????????state?????????????????????????????????
    searchData: {}, // ??????api????????????????????????state????????????????????????
    message: {}
  },
  watch: {
    message (newValue, oldValue) {
      console.log('newValue=', newValue)
      this.formItem.state = [...newValue.state]
      // this.stateChanged(newValue.state, true)
      this.formItem.action = [...newValue.action]
      // this.formItem.selectedSearch = [...newValue.selectedSearch] // ????????????????????? ???????????????????????????
      this.formItem.selected_query = [...newValue.selected_query]
      this.formItem.selected_result = [...newValue.selected_result]
      this.formItem.response = newValue.response
      this.actionBackup = [...this.message.action]
    }
  },
  data () {
    return {
      formItem: {
      //   // selectedSearch: [...this.message.selectedSearch], // ????????????????????? ???????????????????????????
        selected_query: [...this.message.selected_query],
        selected_result: [...this.message.selected_result],
        state: [...this.message.state],
        action: [...this.message.action],
        response: this.message.response
      },
      rules: {
        state: [{
          required: true,
          validator: (rule, value, callback) => {
            if (this.formItem.action && !calAllow(this.actions.system, this.formItem.action, 0) && (!value || value.length === 0)) {
              return callback(new Error('Please select state!'))
            } else callback()
          },
          trigger: 'blur'
        }],
        action: [{
          required: true,
          validator: (rule, value, callback) => {
            if (!value || value.length === 0) {
              return callback(new Error('Please select action!'))
            } else callback()
          },
          trigger: 'blur'
        }],
        response: [{
          required: true,
          type: 'string',
          message: 'Please fill in response!',
          trigger: 'blur'
        }],
        selected_query: [{
          validator: (rule, value, callback) => {
            let active = this.$refs.searchPanel.active
            if (this.formItem.action && !calAllow(this.actions.system, this.formItem.action, 0) && active && active.length > 0 && (!value || value.length === 0)) {
              return callback(new Error('Please select search results!'))
            } else { callback() }
          },
          trigger: 'blur'
        }],
        selected_result: [{
          validator: (rule, value, callback) => {
            let active = this.$refs.searchPanel.active
            if (this.formItem.action && !calAllow(this.actions.system, this.formItem.action, 0) && active && active.length > 0 && (!value || value.length === 0)) {
              return callback(new Error('Please select search results!'))
            } else { callback() }
          },
          trigger: 'blur'
        }]
      },
      drawer: false,
      sendLog: true,
      searchPanelLoading: false,
      searchResults: {},
      actionBackup: [...this.message.action], // ?????????sys action???????????????
      submitting: false // ?????????????????????????????????????????????????????????
    }
  },
  methods: {
    reset () {
      this.sendLog = false // ???????????????????????????????????????log
      this.formItem = {
        selected_query: [],
        selected_result: [],
        state: [],
        action: [],
        response: ''
      }
      this.drawer = false
      this.searchPanelLoading = false
      this.searchResults = {}
      this.actionBackup = []
      this.$nextTick(() => {
        this.sendLog = true
      })
    },
    cascaderVisableChange (show) {
      if (show) { this.drawer = true }
    },
    stateChanged (states, force) {
      if ((this.submitting) && !force) return
      this.searchPanelLoading = true
      this.searchResults = {}
      // this.searchData(states || [], (data) => {
      //   this.searchPanelLoading = false
      //   this.searchResults = {...data}
      // })
      // ??????action???selectedSearch
      let originSendlog = this.sendLog
      this.sendLog = false
      this.$nextTick(() => {
        this.sendLog = originSendlog
      })
      // ??????????????????????????????????????????????????????log
      if (this.sendLog && !this.sendDisabled && this.conversationId && !force) {
        this.$log({
          conversationId: this.conversationId,
          type: 'stateChanged',
          data: {states}
        })
      }
    },
    actionChanged (actions) {
      // ????????????????????????????????????action?????????????????????????????????search result
      // if (this.role === 'sys' && this.formItem.selectedSearch.length > 0) {
      //   // actionBackup?????????????????????sys action???????????????
      // // ???????????????action?????????????????????????????????????????????selectedSearch????????????????????????????????????result
      //   if (this.actionBackup[0] !== actions[0]) {
      //     this.formItem.selectedSearch = []
      //   }
      //   this.actionBackup = [...actions]
      // }
      if (this.sendLog && !this.sendDisabled && this.conversationId) {
        this.$log({
          conversationId: this.conversationId,
          type: 'actionChanged',
          data: {actions}
        })
      }
    },
    responseChanged () {
      if (this.sendLog && !this.sendDisabled && this.conversationId && this.formItem.response) {
        this.$log({
          conversationId: this.conversationId,
          type: 'responseChanged',
          data: {response: this.formItem.response}
        })
      }
    },
    selectChanged () {
      console.log('????????????formItem', this.formItem)
    },
    showTooltip (item) {
      if (!this.sendDisabled && this.sendLog && this.conversationId) {
        this.$log({
          conversationId: this.conversationId,
          type: 'showTooltip',
          data: {item: {...item}}
        })
      }
    },
    canCreateState (value) {
      if (value.split(' ').length >= 5) {
        return {
          canCreate: false,
          message: 'Too many words!'
        }
      }
      return {
        canCreate: true
      }
    },
    getUpdated () {
      let temp = { ...this.message }
      temp.state = [...this.formItem.state]
      temp.action = [...this.formItem.action]
      temp.selected_query = [...this.formItem.selected_query]
      temp.selected_result = [...this.formItem.selected_result]
      temp.response = this.formItem.response
      console.log('temp=', temp)
      return temp
    }
  }
}
</script>
