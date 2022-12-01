<template>
  <div style="width:100%;height:100%">
    <div v-show="showFrame" style="width:100%;height:100%">
      <Button type="primary" @click="showFrame=false">
        <Icon type="ios-arrow-back" />Back to results
      </Button>
      <div style="width:100%;height: calc( 100% - 50px );margin-top:10px">
        <Spin size="large" v-if="frameLoading" fix>
          <Icon type="ios-loading" size="50" class="spin-icon-load"></Icon>
          <div>Loading page...</div>
        </Spin>
        <iframe
          name="frame"
          :src="pageLink"
          style="width:100%;height:100%"
          ref="frame"
          frameborder="0"
        />
      </div>
    </div>
    <div v-show="!showFrame">
      <Spin size="large" v-if="loading" fix>
        <Icon type="ios-loading" size="50" class="spin-icon-load"></Icon>
        <div>Updating filters...</div>
      </Spin>
      <Button @click="searchDataWithFilters">Search with filters</Button>
      <ul style="list-style-type: none" v-for="(actions,name) in data" v-if="name==='Filters'" :key="name">
        <li v-for="item in actions" :key="item.value">
          <input type="checkbox" :id="item.id" :value="item.value" v-model="selectedFilters">
          <label :for="item.id">{{item.name}}</label>
        </li>
      </ul>
    </div>
  </div>
</template>
<script>

const arrayEaquals = (arr1, arr2) => {
  // 比较两个数组，内容相同，顺序也相同，返回true，内容相同顺序不同返回‘orderChange’
  if (arr1.length !== arr2.length) return false
  if (JSON.stringify(arr1) === JSON.stringify(arr2)) return true
  let difference = arr1.concat(arr2).filter(v => !arr1.includes(v) || !arr2.includes(v))
  return difference.length === 0 ? 'orderChange' : false
}
export default {
  props: ['value', 'activeActions', 'loading', 'data', 'searchResultConfig'],
  model: {
    prop: 'value',
    event: 'change'
  },
  created () {
    if (this.value.length > 0) {
      this.value.map(val => {
        this.selected.push(val.itemId)
      })
    }
  },
  mounted () {
    this.bindLoadEvent()
  },
  watch: {
    value (newVal, oldVal) {
      try {
        // eslint-disable-next-line
        let x = newVal[0]['content']
      } catch (error) {
        let equals = arrayEaquals(newVal.map(v => v.itemId), oldVal.map(v => v.itemId))
        if (equals === true) return
        this.selected = newVal.map(v => v.itemId)
        // value改动，说明model的值改动,就可以触发事件
        // 事件有两种，一种是改了顺序，一种是改了选项
        if (equals === false) { this.$emit('on-filters-change', newVal) }
        if (equals === 'orderChange') { this.$emit('on-order-change', newVal) }
      }
    },
    activeActions (newVal) {
      if (!newVal) return
      let currentActive = []
      for (let activeItem in this.searchResultConfig) {
        this.searchResultConfig[activeItem].indexOf(newVal[0]) >= 0 && currentActive.push(activeItem)
      }
      this.active = [...currentActive]
    },
    data (newVal) {
      this.collapse = Object.keys(newVal)
    }
  },
  data () {
    return {
      selected: [],
      collapse: [],
      active: '',
      pageLink: '',
      showFrame: false,
      frameLoading: false,
      selectedFilters: []
    }
  },
  methods: {
    searchDataWithFilters () {
      let checkedData = []
      for (let i = 0; i < this.selectedFilters.length; i++) {
        let val = this.selectedFilters[i]
        if (val) {
          checkedData.push(val)
        }
      }
      debugger
      console.log(checkedData)
      this.$emit('on-filters-change', checkedData)
    },
    bindLoadEvent () {
      const iframe = this.$refs.frame
      iframe.onload = () => {
        this.frameLoading = false
      }
      iframe.onreadystatechange = () => {
        if (iframe.readyState === 'interactive' || iframe.readyState === 'complete' || iframe.readyState === 'loaded') {
          this.frameLoading = false
        }
      }
    },
    getActive () {
      return this.active
    },
    onCheckGroupChange (checked) {
      let checkedData = []
      for (var i = 0; i < checked.length; i++) {
        let val = checked[i]
        if (val) {
          if (val.startsWith('n:')) {
            checkedData.push(val.split('||')[1])
          } else {
            checkedData.push(val)
          }
        }
      }
      console.log('NEW FILTERS LIST: ' + checkedData)
      this.selected = checkedData
      // this.$emit('change', checkedData)
    },
    reset () {
      this.selected = []
      this.collapse = []
      this.active = ''
      this.showFrame = false
      this.pageLink = ''
      this.frameLoading = false
    }
  }

}
</script>
