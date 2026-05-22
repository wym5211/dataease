import { useI18n } from '@/hooks/web/useI18n'
import {
  L7PlotChartView,
  L7PlotDrawOptions
} from '@/views/chart/components/js/panel/types/impl/l7plot'
import { Choropleth, ChoroplethOptions } from '@antv/l7plot/dist/esm/plots/choropleth'
import { Dot, DotOptions } from '@antv/l7plot'
import {
  MAP_AXIS_TYPE,
  MAP_EDITOR_PROPERTY,
  MAP_EDITOR_PROPERTY_INNER,
  MapMouseEvent,
  ensureDrillDimensionList
} from '@/views/chart/components/js/panel/charts/map/common'
import { flow, getGeoJsonFile, hexColorToRGBA, parseJson } from '@/views/chart/components/js/util'
import { cloneDeep, isEmpty } from 'lodash-es'
import { FeatureCollection } from '@antv/l7plot/dist/esm/plots/choropleth/types'
import {
  handleGeoJson,
  mapRendered,
  mapRendering
} from '@/views/chart/components/js/panel/common/common_antv'
import { valueFormatter } from '@/views/chart/components/js/formatter'
import { deepCopy } from '@/utils/utils'
import { configCarouselTooltip } from '@/views/chart/components/js/panel/charts/map/tooltip-carousel'
import { getCustomGeoArea } from '@/api/map'
import { TextLayer } from '@antv/l7plot/dist/esm'
import { centroid } from '@turf/centroid'

const { t } = useI18n()

type BubbleMapRow = MapRow

type BubbleMapContext = {
  drawOption: L7PlotDrawOptions<Choropleth>
  geoJson: FeatureCollection
  customSubArea: CustomGeoSubArea[]
  layers?: TextLayer[]
  geoJsonMap?: Record<string, FeatureCollection['features'][number]>
}

type SceneMapWithControls = {
  _canvasContainer?: HTMLElement
  keyboard?: {
    disable?: () => void
  }
}

/**
 * 气泡地图
 */
export class BubbleMap extends L7PlotChartView<ChoroplethOptions, Choropleth> {
  properties: EditorProperty[] = [...MAP_EDITOR_PROPERTY, 'bubble-animate']
  propertyInner = {
    ...MAP_EDITOR_PROPERTY_INNER,
    'tooltip-selector': [...MAP_EDITOR_PROPERTY_INNER['tooltip-selector'], 'carousel'],
    'basic-style-selector': [...MAP_EDITOR_PROPERTY_INNER['basic-style-selector'], 'areaBaseColor']
  }
  axis = MAP_AXIS_TYPE
  axisConfig: AxisConfig = {
    xAxis: {
      name: `${t('chart.area')} / ${t('chart.dimension')}`,
      type: 'd',
      limit: 1
    },
    yAxis: {
      name: `${t('chart.bubble_size')} / ${t('chart.quota')}`,
      type: 'q',
      limit: 1
    }
  }
  constructor() {
    super('bubble-map')
  }

  async drawChart(drawOption: L7PlotDrawOptions<Choropleth>): Promise<Choropleth> {
    const { chart, level, areaId, container, action, scope } = drawOption
    if (!areaId) {
      return
    }
    chart.container = container
    let geoJson = {} as FeatureCollection
    let customSubArea: CustomGeoSubArea[] = []
    let data = chart.data?.data as BubbleMapRow[] | undefined
    let geoJsonMap: Record<string, FeatureCollection['features'][number]> | undefined
    if (areaId.startsWith('custom_')) {
      customSubArea = (await getCustomGeoArea(areaId)).data || []
      customSubArea.forEach(a => (a.scopeArr = a.scope?.split(',') || []))
      geoJson = cloneDeep(await getGeoJsonFile('156'))
      geoJsonMap = geoJson.features.reduce<Record<string, FeatureCollection['features'][number]>>(
        (p, n) => {
          if (n.properties['adcode']) {
            p['156' + n.properties['adcode']] = n
          }
          return p
        },
        {}
      )
      const areaNameMap = geoJson.features.reduce<Record<string, string>>((p, n) => {
        p['156' + n.properties.adcode] = n.properties.name
        return p
      }, {})
      const { areaMapping } = parseJson(chart.senior)
      const areaMap = customSubArea.reduce<Record<string, CustomGeoSubArea>>((p, n) => {
        const mappedName = areaMapping?.[areaId]?.[n.name]
        if (mappedName) {
          n.name = mappedName
        }
        p[n.name] = n
        n.scopeArr = n.scope?.split(',') || []
        return p
      }, {})
      const fakeData: BubbleMapRow[] = []
      data?.forEach(d => {
        const area = areaMap[d.name as string]
        if (area) {
          area.scopeArr.forEach(adcode => {
            fakeData.push({
              ...d,
              name: areaNameMap[adcode],
              field: areaNameMap[adcode],
              scope: area.scopeArr,
              areaName: d.name
            })
          })
        }
      })
      data = fakeData
    } else {
      if (scope) {
        geoJson = cloneDeep(await getGeoJsonFile('156'))
        geoJson.features = geoJson.features.filter(f => scope.includes('156' + f.properties.adcode))
      } else {
        geoJson = cloneDeep(await getGeoJsonFile(areaId))
      }
    }
    let options: ChoroplethOptions = {
      preserveDrawingBuffer: true,
      map: {
        type: 'mapbox',
        style: 'blank'
      },
      geoArea: {
        type: 'geojson'
      },
      source: {
        data: data || [],
        joinBy: {
          sourceField: 'name',
          geoField: 'name',
          geoData: geoJson
        }
      },
      viewLevel: {
        level,
        adcode: 'all'
      },
      autoFit: true,
      chinaBorder: false,
      color: {
        field: 'value'
      },
      style: {
        opacity: 1,
        lineWidth: 0.6,
        lineOpacity: 1
      },
      label: {
        field: '_DE_LABEL_',
        style: {
          textAnchor: 'center'
        }
      },
      tooltip: {},
      legend: false,
      // 禁用线上地图数据
      customFetchGeoData: () => null
    }
    const context: BubbleMapContext = { drawOption, geoJson, customSubArea, geoJsonMap }
    options = this.setupOptions(chart, options, context)

    const tooltip = deepCopy(options.tooltip)
    options = { ...options, tooltip: { ...tooltip, showComponent: false } }
    const view = new Choropleth(container, options)
    const dotLayer = this.getDotLayer(chart, geoJson, drawOption, customSubArea, geoJsonMap)
    if (!areaId.startsWith('custom_')) {
      dotLayer.options = { ...dotLayer.options, tooltip }
    }
    const areaAdcodeMap = geoJson.features.reduce<Record<string, string | number>>(
      (map, feature) => {
        const name = feature.properties?.name as string | undefined
        const adcode = feature.properties?.adcode as string | number | undefined
        if (name && adcode != null) {
          map[name] = adcode
        }
        return map
      },
      {}
    )
    this.configZoomButton(chart, view)
    mapRendering(container)
    view.once('loaded', () => {
      // 修改地图鼠标样式为默认
      const sceneMap = view.scene.map as SceneMapWithControls
      const canvasElement = sceneMap._canvasContainer?.lastElementChild as HTMLElement | null
      if (canvasElement) {
        canvasElement.style.cursor = 'default'
      }
      context.layers?.forEach(layer => {
        view.addLayer(layer)
      })
      dotLayer.addToScene(view.scene)
      dotLayer.once('add', () => {
        mapRendered(container)
      })
      sceneMap.keyboard?.disable?.()
      dotLayer.on('dotLayer:click', (ev: MapMouseEvent) => {
        const data = ev.feature.properties as Record<string, any>
        let adcode: string | number | undefined
        let scope: string[] | undefined
        if (areaId.startsWith('custom_')) {
          adcode = '156'
          const area = customSubArea.find(a => a.name === data.name)
          scope = area?.scopeArr
        } else {
          adcode = areaAdcodeMap[data.name as string]
        }
        ensureDrillDimensionList(chart, data)
        action({
          x: ev.x,
          y: ev.y,
          data: {
            data,
            extra: { adcode, scope }
          }
        })
      })
      dotLayer.once('loaded', () => {
        chart.container = container
        configCarouselTooltip(chart, view, data || [], null, customSubArea, drawOption)
      })
    })
    return view
  }

  private getDotLayer(
    chart: Chart,
    geoJson: FeatureCollection,
    drawOption: L7PlotDrawOptions<Choropleth>,
    customSubArea: CustomGeoSubArea[],
    geoJsonMap?: Record<string, FeatureCollection['features'][number]>
  ): Dot {
    const { areaId } = drawOption
    const { basicStyle, tooltip } = parseJson(chart.customAttr)
    const { bubbleCfg } = parseJson(chart.senior)
    const { offsetHeight, offsetWidth } = document.getElementById(drawOption.container)
    const sourceData = (chart.data?.data || []) as BubbleMapRow[]
    const dotData: Array<{
      name: string
      size: number | string
      properties: BubbleMapRow
      x: number
      y: number
    }> = []
    const options: DotOptions = {
      source: {
        data: dotData,
        parser: {
          type: 'json',
          x: 'x',
          y: 'y'
        }
      },
      shape: 'circle',
      size: {
        field: 'size',
        value: [5, Math.min(offsetHeight, offsetWidth) / 20]
      },
      visible: true,
      zIndex: 0.05,
      color: hexColorToRGBA(basicStyle.colors[0], basicStyle.alpha),
      name: 'bubbleLayer',
      style: {
        opacity: 1
      },
      state: {
        active: { color: 'rgba(30,90,255,1)' }
      },
      tooltip: {
        showComponent: tooltip.show
      }
    }
    if (areaId.startsWith('custom_')) {
      const { areaMapping } = parseJson(chart.senior)
      const customAreaMap = customSubArea.reduce<Record<string, CustomGeoSubArea>>((p, n) => {
        const mappedName = areaMapping?.[areaId]?.[n.name]
        if (mappedName) {
          n.name = mappedName
        }
        p[n.name] = n
        return p
      }, {})
      sourceData.forEach(d => {
        const area = customAreaMap[d.name as string]
        if (area) {
          const areaJsonArr: FeatureCollection['features'] = []
          area.scopeArr?.forEach(adcode => {
            const json = geoJsonMap[adcode]
            json && areaJsonArr.push(json)
          })
          if (areaJsonArr.length) {
            const areaJson: FeatureCollection = {
              type: 'FeatureCollection',
              features: areaJsonArr
            }
            const center = centroid(areaJson)
            // 轮播用
            area.centroid = [center.geometry.coordinates[0], center.geometry.coordinates[1]]
            dotData.push({
              name: area.name,
              size: d.value ?? 0,
              properties: d,
              x: center.geometry.coordinates[0],
              y: center.geometry.coordinates[1]
            })
          }
        }
      })
      if (options.tooltip && options.tooltip.showComponent) {
        options.tooltip.items = ['name', 'adcode', 'value']
        options.tooltip.customTitle = ({ name }) => {
          return name
        }
        const formatterMap = tooltip.seriesTooltipFormatter
          ?.filter(i => i.show)
          .reduce((pre, next) => {
            pre[next.id] = next
            return pre
          }, {}) as Record<string, SeriesFormatter>
        options.tooltip.customItems = originalItem => {
          const result = []
          if (isEmpty(formatterMap)) {
            return result
          }
          const head = originalItem.properties
          const formatter = formatterMap[head.quotaList?.[0]?.id]
          if (!isEmpty(formatter)) {
            const originValue = parseFloat(head.value as string)
            const value = valueFormatter(originValue, formatter.formatterCfg)
            const name = isEmpty(formatter.chartShowName) ? formatter.name : formatter.chartShowName
            result.push({ ...head, name, value: `${value ?? ''}` })
          }
          head.dynamicTooltipValue?.forEach(item => {
            const formatter = formatterMap[item.fieldId]
            if (formatter) {
              const value = valueFormatter(parseFloat(item.value), formatter.formatterCfg)
              const name = isEmpty(formatter.chartShowName)
                ? formatter.name
                : formatter.chartShowName
              result.push({ color: 'grey', name, value: `${value ?? ''}` })
            }
          })
          return result
        }
        options.tooltip.domStyles = {
          'l7plot-tooltip': {
            'background-color': tooltip.backgroundColor,
            'font-size': `${tooltip.fontSize}px`,
            'line-height': 1.6
          },
          'l7plot-tooltip__name': {
            color: tooltip.color
          },
          'l7plot-tooltip__value': {
            color: tooltip.color
          },
          'l7plot-tooltip__title': {
            color: tooltip.color
          }
        }
      }
    } else {
      const areaMap = sourceData.reduce<
        Record<string, { value: BubbleMapRow['value']; data: BubbleMapRow }>
      >((obj, value) => {
        if (value.field != null) {
          obj[String(value.field)] = { value: value.value, data: value }
        }
        return obj
      }, {})
      geoJson.features.forEach(item => {
        const name = item.properties['name']
        const areaItem = areaMap[name]
        if (areaItem && (areaItem.value || areaItem.value === 0)) {
          dotData.push({
            x: item.properties['centroid'][0],
            y: item.properties['centroid'][1],
            size: areaItem.value ?? 0,
            properties: areaItem.data,
            name: name
          })
        }
      })
    }
    if (bubbleCfg && bubbleCfg.enable) {
      return new Dot({
        ...options,
        size: {
          field: 'size',
          value: [10, Math.min(offsetHeight, offsetWidth) / 10]
        },
        animate: {
          enable: true,
          speed: bubbleCfg.speed,
          rings: bubbleCfg.rings
        }
      })
    }
    return new Dot(options)
  }

  private configBasicStyle(
    chart: Chart,
    options: ChoroplethOptions,
    context: Record<string, unknown>
  ): ChoroplethOptions {
    const bubbleContext = context as BubbleMapContext
    const { areaId } = bubbleContext.drawOption
    const geoJson = bubbleContext.geoJson
    const { basicStyle, label } = parseJson(chart.customAttr)
    const senior = parseJson(chart.senior)
    const curAreaNameMapping = senior.areaMapping?.[areaId]
    handleGeoJson(geoJson, curAreaNameMapping)
    options.color = basicStyle.areaBaseColor
    if (!chart.data?.data?.length || !geoJson?.features?.length) {
      options.label && (options.label.field = 'name')
      return options
    }
    const data = chart.data.data as BubbleMapRow[]
    const areaMap = data.reduce<Record<string, BubbleMapRow['value']>>((obj, value) => {
      if (value.field != null) {
        obj[String(value.field)] = value.value
      }
      return obj
    }, {})
    geoJson.features.forEach(item => {
      const name = item.properties['name']
      // trick, maybe move to configLabel, here for perf
      if (label.show) {
        const content = []
        if (label.showDimension) {
          content.push(name)
        }
        if (label.showQuota) {
          ;(areaMap[name] || areaMap[name] === 0) &&
            content.push(valueFormatter(areaMap[name], label.quotaLabelFormatter))
        }
        item.properties['_DE_LABEL_'] = content.join('\n\n')
      }
    })
    return options
  }

  protected configCustomArea(
    chart: Chart,
    options: ChoroplethOptions,
    context: Record<string, unknown>
  ): ChoroplethOptions {
    const bubbleContext = context as BubbleMapContext
    const { drawOption, customSubArea } = bubbleContext
    if (!drawOption.areaId.startsWith('custom_')) {
      return options
    }
    const customAttr = parseJson(chart.customAttr)
    const { label } = customAttr
    const data = (chart.data?.data || []) as BubbleMapRow[]
    const areaMap = data.reduce<Record<string, BubbleMapRow>>((obj, value) => {
      if (value.field != null) {
        obj[String(value.field)] = value
      }
      return obj
    }, {})
    //处理label
    options.label = {
      visible: false
    }
    if (label.show) {
      const geoJsonMap = bubbleContext.geoJsonMap
      const { areaMapping } = parseJson(chart.senior)
      const labelLocation: Array<{ name: string; x: number; y: number }> = []
      customSubArea.forEach(area => {
        const areaJsonArr: FeatureCollection['features'] = []
        area.scopeArr?.forEach(adcode => {
          const json = geoJsonMap[adcode]
          json && areaJsonArr.push(json)
        })
        if (areaJsonArr.length) {
          const areaJson: FeatureCollection = {
            type: 'FeatureCollection',
            features: areaJsonArr
          }
          const content = []
          if (label.showDimension) {
            const mappedName = areaMapping?.[drawOption.areaId]?.[area.name]
            if (mappedName) {
              area.name = mappedName
            }
            content.push(area.name)
          }
          if (label.showQuota) {
            const areaData = areaMap[area.name]
            if (areaData && (areaData.value || areaData.value === 0)) {
              content.push(valueFormatter(areaData.value, label.quotaLabelFormatter))
            }
          }
          const center = centroid(areaJson)
          labelLocation.push({
            name: content.join('\n\n'),
            x: center.geometry.coordinates[0],
            y: center.geometry.coordinates[1]
          })
        }
      })
      const areaLabelLayer = new TextLayer({
        name: 'areaLabelLayer',
        source: {
          data: labelLocation,
          parser: {
            type: 'json',
            x: 'x',
            y: 'y'
          }
        },
        field: 'name',
        zIndex: 0.06,
        style: {
          fill: label.color,
          fontSize: label.fontSize,
          opacity: 1,
          fontWeight: 'bold',
          textAnchor: 'center',
          textAllowOverlap: label.fullDisplay,
          padding: !label.fullDisplay ? [2, 2] : undefined
        }
      })
      bubbleContext.layers = [areaLabelLayer]
    }
    return options
  }

  protected setupOptions(
    chart: Chart,
    options: ChoroplethOptions,
    context: Record<string, unknown>
  ): ChoroplethOptions {
    return flow(
      this.configEmptyDataStrategy,
      this.configLabel,
      this.configStyle,
      this.configTooltip,
      this.configBasicStyle,
      this.configCustomArea
    )(chart, options, context, this)
  }
}
